(ns tarantool-clj.client-test
  (:require [tarantool-clj
             [client :as client]
             [tuple-space :as tuple-space]
             [space :as space]
             [test-utils :refer [*system* with-system with-truncated-tarantool]]]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]))

(defn client [] (get *system* client/client))

(defn tester-tuple-space*
  [config]
  (tuple-space/tuple-space (-> config :tester-tuple-space)))

(defn tester-space*
  [config]
  (space/space (-> config :tester-space)))

(use-fixtures :each
  (with-system [client/client])
  with-truncated-tarantool
  (with-system
    [tester-tuple-space* tester-space*]
    {}))

(deftest tuple-space
  (let [space (get *system* tester-tuple-space*)]
    (are [x y] (= x y)
      (tuple-space/insert space [1 "Steve" "Buscemi"])
      [[1 "Steve" "Buscemi"]]

      (tuple-space/insert space [2 "Steve" "Jobs"])
      [[2 "Steve" "Jobs"]]

      (tuple-space/insert space [3 "Tim" "Roth"])
      [[3 "Tim" "Roth"]]

      (tuple-space/select space 0 [1])
      [[1 "Steve" "Buscemi"]]

      (tuple-space/select space 1 ["Steve"] {:iterator :eq})
      [[1 "Steve" "Buscemi"] [2 "Steve" "Jobs"]]

      (tuple-space/delete space [1])
      [[1 "Steve" "Buscemi"]]

      (tuple-space/select space 1 ["Steve"] {:iterator :eq})
      [[2 "Steve" "Jobs"]]

      (tuple-space/update space [2] [["=" 2 "Ballmer"]])
      [[2 "Steve" "Ballmer"]]

      (tuple-space/select space 1 ["Steve"])
      [[2 "Steve" "Ballmer"]])))

(deftest space
  (let [space (get *system* tester-space*)]
    (are [x y] (= x y)
      (space/insert space
                    {:id 1
                     :first-name "Steve"
                     :second-name "Buscemi"})
      '({:id 1 :first-name "Steve" :second-name "Buscemi"})

      (space/insert space
                    {:id 2
                     :first-name "Steve"
                     :second-name "Jobs"})
      '({:id 2 :first-name "Steve" :second-name "Jobs"})

      (space/insert space
                    {:id 3
                     :first-name "Tim"
                     :second-name "Roth"})
      '({:id 3 :first-name "Tim" :second-name "Roth"})

      (space/insert space
                    {:id 4
                     :first-name "Bill"
                     :second-name "Gates"
                     :_tail [1 2 3 4 5]})
      '({:id 4 :first-name "Bill" :second-name "Gates" :_tail (1 2 3 4 5)})

      (space/select-first space
                          {:id 1})
      {:id 1 :first-name "Steve" :second-name "Buscemi"}

      (space/select space
                    {:first-name "Steve"})
      '({:id 1 :first-name "Steve" :second-name "Buscemi"}
        {:id 2 :first-name "Steve" :second-name "Jobs"})

      (space/select space
                    {:first-name "Steve" :second-name "Jobs"})
      '({:id 2 :first-name "Steve" :second-name "Jobs"})

      (space/select space
                    {:first-name "Steve"}
                    {:iterator :eq})
      '({:id 1 :first-name "Steve" :second-name "Buscemi"}
        {:id 2 :first-name "Steve" :second-name "Jobs"})

      (space/select space
                    {:first-name "Steve"}
                    {:iterator :eq :offset 1 :limit 100})
      '({:id 2 :first-name "Steve" :second-name "Jobs"})

      (space/update space
                    {:id 2}
                    {:second-name ["=" "Ballmer"]})
      '({:id 2 :first-name "Steve" :second-name "Ballmer"})

      (space/update space
                    {:id 2}
                    {:second-name [":" 3 4 "dwin"]
                     :first-name [":" 3 4 "phen"]})
      '({:id 2 :first-name "Stephen" :second-name "Baldwin"})

      (space/delete space
                    {:id 3})
      '({:id 3 :first-name "Tim" :second-name "Roth"})

      (space/replace space
                     {:id 4 :first-name "Bill" :second-name "Murray"})
      '({:id 4 :first-name "Bill" :second-name "Murray"})

      (space/eval space
                  "function ping()
                        return 'pong'
                      end")
      []

      (space/call space
                  "ping")
      [["pong"]])))
