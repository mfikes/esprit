(ns esprit.indicators
  (:require [esprit.board]))

(def ^:private print-led js/D32)
(def ^:private eval-led js/D33)
(def ^:private read-led js/D14)
(def ^:private conn-led js/D13)

(defn- pwm [led on-duty-cycle freq]
  (js/analogWrite led (- 1 on-duty-cycle) #js {:freq freq}))

(defn- turn-off [led]
  (.write led true))

(defn- turn-on [led]
  (.write led false))

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
  (js/digitalPulse read-led true #js [100 100]))

(defn indicate-print []
  (js/digitalPulse print-led true #js [100 100]))
