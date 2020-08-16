(ns esprit.board
  (:require-macros [esprit.board :refer [read-board-file]]))

; Read in the board file
(def board (read-board-file))

(defmulti board-item "Initialize a board-item based on its type" :type)

; Basic pin configuration

(defmethod board-item :output-analog
  [{pin-name :pin
    :as item}]
  (let [pin (js/Pin pin-name)]
    (.mode pin "output")
    (when-let [val (:value item)]
      (js/analogWrite pin val #js{:freq (:freq item)
                                  :soft (:soft item)
                                  :forceSoft (:force-soft item)}))
    pin))

(defmethod board-item :output-digital
  [{pin-name :pin
    :as item}]
  (let [pin (js/Pin pin-name)]
    (.mode pin "output")
    (when-let [val (:value item)]
      (.write pin val))
    pin))

(defmethod board-item :input
  [{pin-name :pin}]
  (doto (js/Pin pin-name)
    (.mode "input")))

(defmethod board-item :analog
  [{pin-name :pin}]
  (doto (js/Pin pin-name)
    (.mode "analog")))

(defmethod board-item :input-pullup
  [{pin-name :pin}]
  (doto (js/Pin pin-name)
    (.mode "input-pullup")))

(defmethod board-item :input-pulldown
  [{pin-name :pin}]
  (doto (js/Pin pin-name)
    (.mode "input-pulldown")))

; Peripherals

(defmethod board-item :serial
  [{baud :baud
    :as item}]
  (doto js/Serial2
    (.setup baud #js{:rx (:rx item)
                     :tx (:tx item)
                     :ck (:ck item)
                     :cts (:cts item)
                     :bytesize (:bytesize item)
                     :parity (:parity item)
                     :stopbits (:stopbits item)
                     :flow (:flow item)})))

(defmethod board-item :software-serial
  [{baud :baud
    :as item}]
  (doto (js/Serial.)
    (.setup baud #js{:rx (:rx item)
                     :tx (:tx item)
                     :bytesize (:bytesize item)
                     :stopbits (:stopbits item)})))

(def items (into {} (map (fn [[key item]] [key (board-item item)])) board))
