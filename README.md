# tarantool-clj

A wrapper for [Java Tarantool Connector](https://github.com/tarantool/tarantool-java)

## Install

https://clojars.org/tarantool-clj

## Usage

```clojure
  (require '[tarantool-clj.client :as client]
           '[tarantool-clj.space :as space]
           '[com.stuartsierra.component :as component])

  (def connection-config
    {:host "127.0.0.1"
     :port 3301
     :username "test"
     :password "test"})

  (def test-space-config
    {:name "test"
     :fields [:id :first-name :second-name]
     :tail :_tail}

  (let [client (-> connection-config 
                   (client/new-client) 
                   (component/start))
        space (-> client
                  (space/new-space test-space-config)
                  (component/start))]
    (space/insert space
                  {:id 1
                   :first-name "Steve"
                   :second-name "Buscemi"})
    ;; ({:id 1 :first-name "Steve" :second-name "Buscemi"})
    (space/insert space
                  {:id 2
                   :first-name "Steve"
                   :second-name "Jobs"})
    ;; ({:id 2 :first-name "Steve" :second-name "Jobs"})
    (space/insert space 
                  {:id 3
                   :first-name "Tim"
                   :second-name "Roth"})
    ;; ({:id 3 :first-name "Tim" :second-name "Roth"})
    (space/insert space 
                  {:id 4
                   :first-name "Bill"
                   :second-name "Gates"
                   :_tail ["some" "other" "values" 42]})
    ;; ({:id 4 :first-name "Bill" :second-name "Gates" 
    ;;                            :_tail ["some" "other" "values" 42]})
    (space/select-first space 
                        {:id 1})
    ;; {:id 1 :first-name "Steve" :second-name "Buscemi"}
    (space/select space 
                  {:first-name "Steve"})
    ;; ({:id 1 :first-name "Steve" :second-name "Buscemi"}
    ;;  {:id 2 :first-name "Steve" :second-name "Jobs"})
    (space/select space 
                  {:first-name "Steve"} 
                  {:iterator :eq})
    ;; ({:id 1 :first-name "Steve" :second-name "Buscemi"}
    ;;  {:id 2 :first-name "Steve" :second-name "Jobs"})
    (space/select space 
                  {:first-name "Steve"} 
                  {:iterator :eq :offset 1 :limit 100})
    ;; ({:id 2 :first-name "Steve" :second-name "Jobs"})
    (space/update space 
                  {:id 2} 
                  {:second-name ["=" "Ballmer"]})
    ;; ({:id 2 :first-name "Steve" :second-name "Ballmer"})
    (space/update space
                  {:id 2} 
                  {:second-name [":" 3 4 "dwin"]
                   :first-name [":" 3 4 "phen"]})
    ;; ({:id 2 :first-name "Stephen" :second-name "Baldwin"})
    (space/delete space
                  {:id 3})
    ;; ({:id 3 :first-name "Tim" :second-name "Roth"})
    (space/eval space
                "function ping()
                   return 'pong'
                 end")
    ;; []
    (space/call space
                "ping")
    ;; [["pong"]]
    (component/stop client))
```

PS: Some of functionality is broken because of underlying Java implementation.
