(ns esprit.repl
  (:require
    [clojure.string :as string]))

(def ^:private net (js/require "net"))

(def ^:private wifi (js/require "Wifi"))

(def ^:private accum (atom ""))

(def ^:private eval-me (atom false))

(defn- write [c o]
  #_(.log js/console "writing" o)
  (doto c
    (.write (.stringify js/JSON o))
    (.write "\0")))

(defn- handle-repl-connection [c]
  (.log js/console "New REPL Connection")
  (.on c "data"
    (fn [data]
      (when (string/starts-with? data "(function (){try{return cljs.core.pr_str")
        (reset! eval-me true))
      (if-not (string/ends-with? data "\0")
        (swap! accum str data)
        (cond
          @eval-me
          (let [response (try
                           #js {:status "success"
                                :value  (js/eval (str @accum data))}
                           (catch :default ex
                             #js {:status     "exception"
                                  :value      (str ex)
                                  :stacktrace (.-stack ex)}))]
            (reset! eval-me false)
            (reset! accum "")
            (write c response))

          (= data ":cljs/quit")
          (.end c)

          :else
          (write c #js {:status "success"
                        :value  "true"}))))))

(def ^:private server (.createServer net handle-repl-connection))

(defn- prompt-dns-sd []
  (.log js/console "Ensure this ESP32's REPL is being advertised via MDNS. For example, on macOS:")
  (.log js/console "dns-sd -P \"Ambly ESP32 WROVER\" _http._tcp local 53001 ambly.local" (.. wifi getIP -ip)))

(defn ensure-cljs-user []
  (when-not (exists? js/cljs.user)
    (set! (.-user js/cljs) #js {})))

(defn- start-server [port]
  (.log js/console "Ready for REPL Connections")
  (prompt-dns-sd)
  (ensure-cljs-user)
  (.listen server port))

(.on wifi "connected" #(start-server 53000))

;; workaround a bug where last form doesn't seem to be evaluated
(def ^:private dummy 3)
