(ns esprit.indicators
  (:require [esprit.board :as board]))

(def ^:private print-led (::print-led board/items))
(def ^:private eval-led (::eval-led board/items))
(def ^:private read-led (::read-led board/items))
(def ^:private conn-led (::conn-led board/items))

(defn- pwm [led on-duty-cycle freq]
  (when led
    (js/analogWrite led (- 1 on-duty-cycle) #js {:freq freq})))

(defn- turn-off [led]
  (when led
    (.write led true)))

(defn- turn-on [led]
  (when led
    (.write led false)))

(defn indicate-eval [eval?]
  (if eval?
    (pwm eval-led 0.8 2)
    (turn-off eval-led)))

(defn indicate-connections
  [connections]
  (if (pos? connections)
    (turn-on conn-led)
    (pwm conn-led 0.02 1)))

(defn indicate-joining-wifi []
  (pwm conn-led 0.5 120))

(defn indicate-no-wifi-creds []
  (pwm conn-led 0.5 2))

(defn indicate-read []
  (when read-led
    (js/digitalPulse read-led true #js [100 100])))

(defn indicate-print []
  (when print-led
    (js/digitalPulse print-led true #js [100 100])))
