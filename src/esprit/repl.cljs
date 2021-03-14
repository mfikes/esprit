(ns esprit.repl
  (:require
   [clojure.string :as string]
   [esprit.indicators :as ind]))

(ind/indicate-eval false)

(def connections (atom 0))

(def ^:private net (js/require "net"))

(def ^:private wifi (js/require "Wifi"))

(defn- write [c o]
  (ind/indicate-eval false)
  (doto c
     (.write (.stringify js/JSON o))
     (.write "\0"))
  (ind/indicate-print))

(defn eval-data [data]
  (try
    (ind/indicate-eval true)
    #js {:status "success"
         :value  (js/eval data)}
    (catch :default ex
      #js {:status "exception"
           :value (str ex)
           :stacktrace (.-stack ex)})))

(defn- handle-repl-connection [c]
  (.log js/console "New REPL Connection")
  (ind/indicate-connections (swap! connections inc))
  (.on c "close"
       (fn []
         (.log js/console "REPL disconnected")
         (ind/indicate-connections (swap! connections dec))))
  (let [buffer (atom "")]
    (.on c "data"
         (fn [data]
           (ind/indicate-read)
           (if-not (string/ends-with? data "\0")
             (swap! buffer str data)
             (let [data (str @buffer data)]
               (reset! buffer "")
               (cond
                 (string/starts-with? data "(function (){try{return cljs.core.pr_str")
                 (write c (eval-data data))

                 (= data ":cljs/quit")
                 (.end c)

                 :else
                 (write c #js {:status "success"
                               :value  "true"}))))))))

(def ^:private server (.createServer net handle-repl-connection))

(defn- display-connect-info []
  (.log js/console "Establish an Esprit REPL by executing")
  (.log js/console (str "clj -M -m cljs.main -re esprit -ro '{:endpoint-address \"" (.. wifi getIP -ip) "\"}' -r")))

(defn ensure-cljs-user []
  (when-not (exists? js/cljs.user)
    (set! (.-user js/cljs) #js {})))

(defn- start-server [port]
  (.log js/console "Ready for REPL Connections")
  (ind/indicate-connections 0)
  (display-connect-info)
  (ensure-cljs-user)
  (.listen server port))

(.on wifi "connected" #(start-server 53000))

(goog-define wifi-ssid "")
(goog-define wifi-password "")

(if (seq wifi-ssid)
  (do
    (ind/indicate-joining-wifi)
    (.connect wifi wifi-ssid #js {:password wifi-password} (fn [])))
  (ind/indicate-no-wifi-creds))
