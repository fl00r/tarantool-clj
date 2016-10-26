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
  (with-system
    [tester-tuple-space* tester-space*]
    {})
  with-truncated-tarantool)

(deftest tuple-space
  (let [space (get *system* tester-tuple-space*)]
    (prn (tuple-space/insert space [1 "Steve" "Buscemi"]))
    (prn (tuple-space/insert space [2 "Steve" "Jobs"]))
    (prn (tuple-space/insert space [3 "Tim" "Roth"]))
    (prn (tuple-space/select space 0 [1]))
    (prn (tuple-space/select space 1 ["Steve"] {:iterator :eq}))
    (prn (tuple-space/delete space [1]))
    (prn (tuple-space/select space 1 ["Steve"] {:iterator :eq}))
    (prn (tuple-space/update space [2] [["=" 2 "Ballmer"]]))
    (prn (tuple-space/select space 1 ["Steve"]))))

(deftest space
  (let [space (get *system* tester-space*)]
    (is (=
         (space/insert space
                       {:id 1
                        :first-name "Steve"
                        :second-name "Buscemi"})
         '({:id 1 :first-name "Steve" :second-name "Buscemi"})))
    (is (=
         (space/insert space
                        {:id 2
                         :first-name "Steve"
                         :second-name "Jobs"})
         '({:id 2 :first-name "Steve" :second-name "Jobs"})))
    (is (=
         (space/insert space
                     {:id 3
                      :first-name "Tim"
                      :second-name "Roth"})
         '({:id 3 :first-name "Tim" :second-name "Roth"})))
    (is (=
         (space/insert space
                        {:id 4
                         :first-name "Bill"
                         :second-name "Gates"
                         :_tail [1 2 3 4 5]})
         '({:id 4, :first-name "Bill", :second-name "Gates"})))
    (is (space/select-first space
                            {:id 1})
        {:id 1 :first-name "Steve" :second-name "Buscemi"})
    (is (=
         (space/select space
                       {:first-name "Steve"})
         '({:id 1 :first-name "Steve" :second-name "Buscemi"}
           {:id 2 :first-name "Steve" :second-name "Jobs"})))
    (is (=
         (space/select space
                        {:first-name "Steve"}
                        {:iterator :eq})
         '({:id 1 :first-name "Steve" :second-name "Buscemi"}
           {:id 2 :first-name "Steve" :second-name "Jobs"})))
    (is (=
         (space/select space
                        {:first-name "Steve"}
                        {:iterator :eq :offset 1 :limit 100})
         '({:id 2 :first-name "Steve" :second-name "Jobs"})))
    (is (=
         (space/update space
                        {:id 2}
                        {:second-name ["=" "Ballmer"]})
         '({:id 2 :first-name "Steve" :second-name "Ballmer"})))
    (is (=
         (space/update space
                        {:id 2}
                        {:second-name [":" 3 4 "dwin"]
                         :first-name [":" 3 4 "phen"]})
         '({:id 2 :first-name "Stephen" :second-name "Baldwin"})))
    (is (=
         (space/delete space
                       {:id 3})
         '({:id 3 :first-name "Tim" :second-name "Roth"})))
    (is (=
         (space/eval space
                       "function ping()
                        return 'pong'
                      end")
         []))
    (is (=
         (space/call space
                     "ping")
         [["pong"]]))))
