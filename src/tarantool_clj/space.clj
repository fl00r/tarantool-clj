(ns tarantool-clj.space
  (:require [tarantool-clj
             [tuple-space :as tuple-space]
             [client :as client]
             [constants :as constants]]
            [defcomponent :refer [defcomponent]])
  (:refer-clojure :exclude [eval update replace]))

(defprotocol SpaceProtocol
  (select-first
    [this key-doc]
    [this key-doc opts])
  (select
    [this key-doc]
    [this key-doc opts])
  (insert [this data-doc])
  (replace [this data-doc])
  (update [this key-doc ops-doc])
  (delete [this key-doc])
  (upsert [this data-tuple ops-tuples])
  (call
    [this function-name]
    [this function-name args])
  (eval
    [this expression]
    [this expression args]))

(defn get-space-id
  [{:keys [client space-config]}]
  (let [space-name (:name space-config)
        spaces-tuple-space (tuple-space/spaces-tuple-space client)]
    (when-not space-name
      (throw (Exception. "You should specify space name")))
    (-> spaces-tuple-space
        (tuple-space/select 2 [space-name])
        (first)
        (first)
        (or (throw (Exception. (str "Unknown space name: " space-name)))))))

(defn parse-indexes
  [indexes {:keys [fields]}]
  (let [max-n (count fields)]
    (->> indexes
         (map
          (fn [[_ index-id _ _ _ parts]]
            (let [parts->fields
                  (doall
                   (map (fn [[n _]]
                          (when (>= n max-n)
                            (throw (Exception.
                                    (str "field-no exceeded number of fields you passed: field-no "
                                         n ", fields: " fields))))
                          (nth fields n))
                        parts))]
              [index-id parts->fields])))
         (into {}))))

(defn get-index-definitions
  [{:keys [client space-config] :as space} space-id]
  (let [indexes-tuple-space (tuple-space/indexes-tuple-space client)]
    (-> indexes-tuple-space
        (tuple-space/select 0 [space-id])
        (parse-indexes space-config))))

(defn key-doc->index-id
  [space key-doc]
  (let [index-definitions (-> space :index-definitions)
        keys (-> key-doc keys set)
        keys-n (count keys)
        index-id (some (fn [[index-id field-parts]]
                         (when (= keys (->> field-parts (take keys-n) (set)))
                           index-id))
                       index-definitions)]
    (if index-id
      index-id
      (throw
       (Exception.
        (str "Can't find proper index for keys: " keys " in indexes " index-definitions))))))

(defn key-doc->key-tuple
  [space key-doc index-id]
  (let [index-definitions (-> space :index-definitions)
        keys (-> key-doc keys set)
        keys-n (count keys)
        index-keys (get index-definitions index-id)
        ok? (= keys (->> index-keys (take keys-n) (set)))]
    (if ok?
      (->> index-keys
           (map key-doc)
           (filter identity))
      (throw
       (Exception.
        (str "Keys " keys " doesn't match index keys " index-keys))))))

(defn data-doc->data-tuple
  [space doc]
  (let [fields (-> space :space-config :fields)
        tail (-> space :space-config :tail)
        tail-data (when tail (get doc tail))
        tuple (mapv doc fields)]
    (if tail-data
      (concat tuple tail-data)
      tuple)))

(defn data-tuples->data-docs
  [space tuple-data]
  (let [fields (-> space :space-config :fields)
        fields-n (count fields)
        tail (-> space :space-config :tail)]
    (map
     #(let [tuple-n (count %)
            tail-n (- tuple-n fields-n)
            doc (->> % (map vector fields) (into {}))]
        (if (and tail (> tail-n 0))
          (assoc doc tail (take-last tail-n %))
          doc))
     tuple-data)))

(defn ops-doc->ops-tuple
  [space ops-doc]
  (let [fields (-> space :space-config :fields)]
    (map
     (fn [[field [head & tail]]]
       (let [field-id (.indexOf fields field)]
         (conj tail field-id head)))
     ops-doc)))

(defcomponent space [client/client]
  [space-config]
  (start [this]
         (let [space-id (get-space-id this)
               index-definitions (get-index-definitions this space-id)
               client (get this :client)
               tuple-space (tuple-space/new-tuple-space client space-id)]
           (assoc this
                  :tuple-space tuple-space
                  :index-definitions index-definitions)))
  (stop [this]
        (dissoc this :tuple-space))
  SpaceProtocol
  (select-first [this key-doc]
                (first (select this key-doc {})))
  (select-first [this key-doc opts]
                (first (select this key-doc opts)))
  (select [this key-doc]
          (select this key-doc {}))
  (select [{:keys [tuple-space] :as this} key-doc opts]
          (let [index-id (or
                          (:index opts)
                          (key-doc->index-id this key-doc))
                key-tuple (key-doc->key-tuple this key-doc index-id)]
            (data-tuples->data-docs
             this
             (tuple-space/select tuple-space index-id key-tuple opts))))
  (insert [{:keys [tuple-space] :as this} data-doc]
          (let [data-tuple (data-doc->data-tuple this data-doc)]
            (data-tuples->data-docs
             this
             (tuple-space/insert tuple-space data-tuple))))
  (replace [{:keys [tuple-space] :as this} data-doc]
           (let [data-tuple (data-doc->data-tuple this data-doc)]
             (data-tuples->data-docs
              this
              (tuple-space/replace tuple-space data-tuple))))
  (update [{:keys [tuple-space] :as this} key-doc ops-doc]
          ;; warn: index-id is not supported in underlying Java client
          (let [index-id (key-doc->index-id this key-doc)
                key-tuple (key-doc->key-tuple this key-doc index-id)
                ops-tuple (ops-doc->ops-tuple this ops-doc)]
            (data-tuples->data-docs
             this
             (tuple-space/update tuple-space key-tuple ops-tuple))))
  (delete [{:keys [tuple-space] :as this} key-doc]
          (let [index-id (key-doc->index-id this key-doc)
                key-tuple (key-doc->key-tuple this key-doc index-id)]
            (data-tuples->data-docs
             this
             (tuple-space/delete tuple-space key-tuple))))
  (upsert [{:keys [tuple-space]} data-tuple ops-tuples]
          ;; upsert is broken
          (tuple-space/upsert tuple-space data-tuple ops-tuples))
  (call [{:keys [tuple-space]} function-name]
        (tuple-space/call tuple-space function-name))
  (call [{:keys [tuple-space]} function-name args]
        (tuple-space/call tuple-space function-name args))
  (eval [{:keys [tuple-space]} expression]
        (tuple-space/eval tuple-space expression))
  (eval [{:keys [tuple-space]} expression args]
        (tuple-space/eval tuple-space expression args)))
