(ns tarantool-clj.client
  (:require [defcomponent :refer [defcomponent]])
  (import [org.tarantool
           TarantoolConnection16Impl
           TarantoolConnection16])
  (:refer-clojure :exclude [eval update replace]))
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

;;
;; Wrapper arround Java Tarantool Connector
;; https://github.com/tarantool/tarantool-java/
;;
(defcomponent client []
  [config]
  (start [this]
         (let [{:keys [host port username password]} (-> config :tarantool)
               conn (TarantoolConnection16Impl. host port)]
           (when (and username password)
             (.auth conn username password))
           (assoc this :conn conn)))
  (stop [this]
        (let [conn (:conn this)]
          (when conn
            (.close conn))
          (dissoc this :conn)))
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
