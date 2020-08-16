(ns esprit.board
  (:require-macros [esprit.board :refer [read-board-file]]))

(def board (read-board-file))
