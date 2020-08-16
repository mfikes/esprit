(ns esprit.board
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defmacro read-board-file []
  (list 'quote (edn/read-string (slurp (io/resource "board.edn")))))
