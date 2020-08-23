(ns esprit.board
  (:require-macros [esprit.board :refer [read-board-file]]))

; Read in the board file
(defonce board (read-board-file))

(defn board-item [{type :type
                   :as item}]
  (case type
    ; Basic Pins
    :output-analog (let [{pin-name :pin} item]
                     (let [pin (js/Pin pin-name)]
                       (.mode pin "output")
                       (when-let [val (:value item)]
                         (js/analogWrite pin val #js{:freq (:freq item)
                                                     :soft (:soft item)
                                                     :forceSoft (:force-soft item)}))
                       pin))
    :output-digital (let [{pin-name :pin} item]
                      (let [pin (js/Pin pin-name)]
                        (.mode pin "output")
                        (when-let [val (:value item)]
                          (.write pin val))
                        pin))
    :input (let [{pin-name :pin} item]
             (doto (js/Pin pin-name)
               (.mode "input")))
    :analog (let [{pin-name :pin} item]
              (doto (js/Pin pin-name)
                (.mode "analog")))
    :input-pullup (let [{pin-name :pin} item]
                    (doto (js/Pin pin-name)
                      (.mode "input_pullup")))
    :input-pulldown (let [{pin-name :pin} item]
                      (doto (js/Pin pin-name)
                        (.mode "input_pulldown")))
    :opendrain (let [{pin-name :pin} item]
                 (let [pin (js/Pin pin-name)]
                   (.mode pin "opendrain")
                   (when-let [val (:value item)]
                     (.write pin val))
                   pin))
    :opendrain-pullup (let [{pin-name :pin} item]
                        (let [pin (js/Pin pin-name)]
                          (.mode pin "opendrain_pullup")
                          (when-let [val (:value item)]
                            (.write pin val))
                          pin))
    ; Peripherals
    :serial (let [{baud :baud} item]
              (doto js/Serial2
                (.setup baud #js{:rx (:rx item)
                                 :tx (:tx item)
                                 :ck (:ck item)
                                 :cts (:cts item)
                                 :bytesize (:bytesize item)
                                 :parity (:parity item)
                                 :stopbits (:stopbits item)
                                 :flow (:flow item)})))
    :software-serial (let [{baud :baud} item]
                       (doto (js/Serial.)
                         (.setup baud #js{:rx (:rx item)
                                          :tx (:tx item)
                                          :bytesize (:bytesize item)
                                          :stopbits (:stopbits item)})))))

(defonce items (into {} (map (fn [[key item]] [key (board-item item)])) board))
