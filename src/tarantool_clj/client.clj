(ns tarantool-clj.client
  (:require [defcomponent :refer [defcomponent]]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log])
  (import [org.tarantool
           TarantoolConnection16Impl
           TarantoolConnection16])
  (:refer-clojure :exclude [eval update replace]))

(defn new-client [])

;;
;; https://tarantool.org/doc/dev_guide/box_protocol.html
;;
(defprotocol ClientProtocol
  (select [this space-id index-id limit offset iterator key-tuple])
  (insert [this space-id data-tuple])
  (replace [this space-id data-tuple])
  (update [this space-id index-id key-tuple ops-tuples])
  (delete [this space-id index-id key-tuple])
  (upsert [this space-id data-tuple ops-tuples])
  (call [this function-name args-tuple])
  (eval [this expression args-tuple]))

(defn create-connection
  [pool]
  (-> pool :config new-client (component/start)))

(defn return-connection
  [{:keys [connections] :as pool} conn]
  (swap! connections (fn [[ready _]]
                       [(conj ready conn)])))

(defn aquire-connection
  [{:keys [connections pool-size] :as pool}]
  (let [new-connections (swap! connections
                               (fn [[[first & tail :as ready] _]]
                                 [tail first]))]
    (let [conn (last new-connections)]
      (if conn
        conn
        (throw (Exception. "Can't aquire connection from pool"))))))

(defn in-pool
  [pool f]
  (let [conn (aquire-connection pool)]
    (try
      (f conn)
      (finally
        (when conn (return-connection pool conn))))))

(defcomponent pool []
  [config]
  (start [this]
         (assoc this
                :connections (atom [(repeatedly (get config :pool-size 20)
                                                #(create-connection this))])))
  (stop [this]
        (->> this :connections (map component/stop))
        (dissoc this :connections))
  ClientProtocol
  (select [this space-id index-id limit offset iterator key-tuple]
          (in-pool this #(select % space-id index-id limit offset iterator key-tuple)))
  (insert [this space-id data-tuple]
          (in-pool this #(insert % space-id data-tuple)))
  (replace [this space-id data-tuple]
           (in-pool this #(replace % space-id data-tuple)))
  (update [this space-id index-id key-tuple ops-tuples]
          (in-pool this #(update % space-id index-id key-tuple ops-tuples)))
  (delete [this space-id index-id key-tuple]
          (in-pool this #(delete % space-id index-id key-tuple)))
  (upsert [this space-id data-tuple ops-tuples]
          (in-pool this #(upsert % space-id data-tuple ops-tuples)))
  (call [this function-name args-tuple]
        (in-pool this #(call % function-name args-tuple)))
  (eval [this expression args-tuple]
        (in-pool this #(eval % expression args-tuple))))

;;
;; Wrapper arround Java Tarantool Connepctor
;; https://github.com/tarantool/tarantool-java/
;;
(defcomponent client []
  [config]
  (start [this]
         (let [{:keys [host port username password start-hook]} config
               conn (TarantoolConnection16Impl. host port)
               this* (assoc this :conn conn)]
           (when (and username password)
             (.auth conn username password))
           (if start-hook
             (start-hook this*)
             this*)))
  (stop [this]
        (let [stop-hook (:stop-hook config)
              conn (:conn this)]
          (when conn
            (.close conn))
          (if stop-hook
            (dissoc (stop-hook this) :conn)
            (dissoc this :conn))))
  ClientProtocol
  (select [{:keys [conn]} space-id index-id limit offset iterator key-tuple]
          (.select conn space-id index-id (to-array key-tuple) offset limit iterator))
  (insert [{:keys [conn]} space-id data-tuple]
          (.insert conn space-id (to-array data-tuple)))
  (replace [{:keys [conn]} space-id data-tuple]
           (.replace conn space-id (to-array data-tuple)))
  ;; in Java connector you can't pass index-id
  ;; so you can use only primary index
  (update [{:keys [conn]} space-id index-id key-tuple ops-tuples]
          (when index-id
            (throw (Exception. "You can't use index here")))
          (.update conn space-id (to-array key-tuple)
                   (to-array (map to-array ops-tuples))))
  ;; the same with delete, you can't pass index-id
  (delete [{:keys [conn]} space-id index-id key-tuple]
          (when index-id
            (throw (Exception. "You can't use index here")))
          (.delete conn space-id (to-array key-tuple)))
  ;; basically it is broken in Java client
  (upsert [{:keys [conn]} space-id data-tuple ops-tuples]
          (throw (Exception. "upsert is not supported yet")))
  (call [{:keys [conn]} function-name args-tuple]
        (.call conn function-name (to-array args-tuple)))
  (eval [{:keys [conn]} expression args-tuple]
        (.eval conn expression (to-array args-tuple))))

(defn new-client
  [config]
  (map->client-record {:config config}))

(defn new-pool
  [config]
  (map->pool-record {:config config}))
