(ns tarantool-clj.constants)

;; https://github.com/tarantool/tarantool/blob/4e06b8e5bd8666bb82ab8f3f92ea6e7ed776bf7c/src/box/index.h#L68
(def ITERATORS
  {:eq 0
   :req 1
   :all 2
   :lt 3
   :le 4
   :ge 5
   :gt 6
   :bits-all-set 7
   :bits-any-set 8
   :overlaps 10
   :neighbor 11})

(def SPACES-SPACE-ID 280)

(def INDEXES-SPACE-ID 288)
