(ns esprit.board
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [config.core :refer [env]]))

(defmacro read-board-file []
  (list 'quote (edn/read-string (slurp (io/resource (or (:board-file env)
                                                        (do (println "NOTE: Defaulting to Esprit board definitions")
                                                            "esprit-esp32-board.edn")))))))
