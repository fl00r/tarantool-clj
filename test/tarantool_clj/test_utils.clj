(ns tarantool-clj.test-utils
  (:require [defcomponent :as defcomponent]
            [com.stuartsierra.component :as component]
            [tarantool-clj.client :as client]))

(def ^:dynamic *system* nil)

(defn with-system
  [components & [additions]]
  (let [config-path "config/test.clj"]
    (fn [f]
      (binding [*system* (defcomponent/system
                           components
                           {:file-config config-path
                            :start true
                            :repo additions})]
        (try
          (f)
          (finally
            (component/stop *system*)))))))


(defn with-truncated-tarantool
  [client f]
  (client/call client "create_testing_space" [])
  (try
    (f)
    (finally (do
               (client/call client "drop_testing_space" [])))))
