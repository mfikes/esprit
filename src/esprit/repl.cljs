(ns esprit.repl
  (:require
    [goog.object :as gobj]
    [clojure.string :as string]))

(def connections (atom 0))

(defn indicate
  [connections]
  (.write js/D21 (zero? connections))
  nil)

(def ^:private net (js/require "net"))

(def ^:private wifi (js/require "Wifi"))

(defn- write [c o]
  ;; Turn off "eval"
  (.write js/D22 true)
  (doto c
    (.write (.stringify js/JSON o))
    (.write "\0"))
  (js/digitalPulse js/D23 true #js [100 100]))

(defn- handle-repl-connection [c]
  (.log js/console "New REPL Connection")
  (indicate (swap! connections inc))
  (.on c "close"
    (fn []
      (.log js/console "REPL disconnected")
      (indicate (swap! connections dec))))
  (let [buffer (atom "")]
    (.on c "data"
      (fn [data]
        (if-not (string/ends-with? data "\0")
          (swap! buffer str data)
          (let [data (str @buffer data)]
            (reset! buffer "")
            (cond
              (string/starts-with? data "(function (){try{return cljs.core.pr_str")
              (let [response (try
                               ;; Indicate "eval"
                               (js/analogWrite js/D22, 0.2 #js {:freq 2})
                               #js {:status "success"
                                    :value  (js/eval data)}
                               (catch :default ex
                                 #js {:status     "exception"
                                      :value      (str ex)
                                      :stacktrace (.-stack ex)}))]
                (write c response))

              (= data ":cljs/quit")
              (.end c)

              :else
              (write c #js {:status "success"
                            :value  "true"}))))))))

(def ^:private server (.createServer net handle-repl-connection))

(defn- prompt-dns-sd []
  (.log js/console "Ensure this ESP32's REPL is being advertised via MDNS. For example, on macOS:")
  (.log js/console "dns-sd -P \"Esprit ESP32 WROVER\" _http._tcp local 53001 esprit.local" (.. wifi getIP -ip)))

(defn ensure-cljs-user []
  (when-not (exists? js/cljs.user)
    (set! (.-user js/cljs) #js {})))

(defn- start-server [port]
  (.log js/console "Ready for REPL Connections")
  (indicate 0)
  (prompt-dns-sd)
  (ensure-cljs-user)
  (.listen server port))

(.on wifi "connected" #(start-server 53000))

(goog-define wifi-ssid "")
(goog-define wifi-password "")

(when (seq wifi-ssid)
  (js/analogWrite js/D21, 0.8 #js {:freq 5})
  (.connect wifi wifi-ssid #js {:password wifi-password} (fn [])))

;; workaround a bug where last form doesn't seem to be evaluated
(def ^:private dummy 3)
