# tarantool-clj

A wrapper for [Java Tarantool Connector](https://github.com/tarantool/tarantool-java)

## Usage

```clojure
  (require '[tarantool-clj.client :as client]
           '[tarantool-clj.space :as space])

  (def connection-config
    {:host "127.0.0.1"
     :port 3301
     :username "test"
     :password "test"})

  (def test-space-config
    {:name "test"
     :fields [:id :first-name :last-name]}

  (let [client (client/new-client connection-config)
        space (space/new-space client test-space-config)]
    (space/insert {:id 1
                   :first-name "Steve"
                   :second-name "Buscemi"})
    (space/insert {:id 2
                   :first-name "Steve"
                   :second-name "Jobs"})
    (space/insert {:id 3
                   :first-name "Tim"
                   :second-name "Roth"})
    (space/select {:id 1})
    (space/select {:first-name "Steve"})
    (space/select {:first-name "Steve"} {:iterator :eq})
    (space/select {:first-name "Steve"} {:terator :eq :offset 1 :limit 100})
    (space/update {:id 2} {:second-name ["=" "Ballmer"]})
    (space/update {:id 1} {:second-name [":" 3 "dwin"]
                           :first-name [":" 3 "phen"]})
    (space/delete {:id 3})
    (client/stop client))
```

PS: Some of functionality is broken because of underlying Java implementation.
