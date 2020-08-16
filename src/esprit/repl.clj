(ns esprit.repl
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [cljs.compiler :as comp]
    [cljs.repl :as repl]
    [clojure.data.json :as json]
    [config.core :refer [env]])
  (:import
    (java.net Socket)
    (java.lang StringBuilder)
    (java.io BufferedReader BufferedWriter IOException)
    (javax.jmdns JmDNS ServiceListener)
    (java.net URI)))

(defn set-logging-level [logger-name level]
  "Sets the logging level for a logger to a level."
  {:pre [(string? logger-name) (instance? java.util.logging.Level level)]}
  (.setLevel (java.util.logging.Logger/getLogger logger-name) level))

(def esprit-bonjour-name-prefix
  "The prefix used in Esprit Bonjour service names."
  "Esprit ")

(defn esprit-bonjour-name? [bonjour-name]
  "Returns true iff a given name is an Esprit Bonjour service name."
  {:pre [(string? bonjour-name)]}
  (.startsWith bonjour-name esprit-bonjour-name-prefix))

(defn bonjour-name->display-name
  "Converts an Esprit Bonjour service name to a display name
  (stripping off esprit-bonjour-name-prefix)."
  [bonjour-name]
  {:pre [(esprit-bonjour-name? bonjour-name)]
   :post [(string? %)]}
  (subs bonjour-name (count esprit-bonjour-name-prefix)))

(defn name-endpoint-map->choice-list [name-endpoint-map]
  "Takes a name to endpoint map, and converts into an indexed list."
  {:pre [(map? name-endpoint-map)]}
  (map vector
    (iterate inc 1)
    (sort-by first name-endpoint-map)))

(defn print-discovered-devices [name-endpoint-map opts]
  "Prints the set of discovered devices given a name endpoint map."
  {:pre [(map? name-endpoint-map) (map? opts)]}
  (if (empty? name-endpoint-map)
    (println "(No devices)")
    (doseq [[choice-number [bonjour-name _]] (name-endpoint-map->choice-list name-endpoint-map)]
      (println (str "[" choice-number "] " (bonjour-name->display-name bonjour-name))))))

(defn setup-mdns
  "Sets up mDNS to populate atom supplied in name-endpoint-map with discoveries.
  Returns a function that will tear down mDNS."
  [reg-type name-endpoint-map]
  {:pre [(string? reg-type)]
   :post [(fn? %)]}
  (let [mdns-service (JmDNS/create)
        service-listener
        (reify ServiceListener
          (serviceAdded [_ service-event]
            (let [type (.getType service-event)
                  name (.getName service-event)]
              (when (and (= reg-type type) (esprit-bonjour-name? name))
                (.requestServiceInfo mdns-service type name 1))))
          (serviceRemoved [_ service-event]
            (swap! name-endpoint-map dissoc (.getName service-event)))
          (serviceResolved [_ service-event]
            (let [type (.getType service-event)
                  name (.getName service-event)]
              (when (and (= reg-type type) (esprit-bonjour-name? name))
                (let [entry {name (let [info (.getInfo service-event)]
                                    {:address (.getHostAddress (.getAddress info))
                                     :port    (.getPort info)})}]
                  (swap! name-endpoint-map merge entry))))))]
    (.addServiceListener mdns-service reg-type service-listener)
    (fn []
      (.removeServiceListener mdns-service reg-type service-listener)
      (.close mdns-service))))

(defn discover-and-choose-device
  "Looks for Esprit devices advertised via Bonjour and presents
  a simple command-line UI letting user pick one, unless
  choose-first-discovered? is set to true in which case the UI is bypassed"
  [choose-first-discovered? opts]
  {:pre [(map? opts)]}
  (let [reg-type "_http._tcp.local."
        name-endpoint-map (atom {})
        tear-down-mdns
        (loop [count 0
               tear-down-mdns (setup-mdns reg-type name-endpoint-map)]
          (if (empty? @name-endpoint-map)
            (do
              (Thread/sleep 100)
              (when (= 20 count)
                (println "\nSearching for devices ..."))
              (if (zero? (rem (inc count) 100))
                (do
                  (tear-down-mdns)
                  (recur (inc count) (setup-mdns reg-type name-endpoint-map)))
                (recur (inc count) tear-down-mdns)))
            tear-down-mdns))]
    (try
      (Thread/sleep 500)                                    ;; Sleep a little more to catch stragglers
      (loop [current-name-endpoint-map @name-endpoint-map]
        (println)
        (print-discovered-devices current-name-endpoint-map opts)
        (when-not choose-first-discovered?
          (println "\n[R] Refresh\n")
          (print "Choice: ")
          (flush))
        (let [choice (if choose-first-discovered? "1" (read-line))]
          (if (= "r" (.toLowerCase choice))
            (recur @name-endpoint-map)
            (let [choices (name-endpoint-map->choice-list current-name-endpoint-map)
                  choice-ndx (try (dec (Long/parseLong choice)) (catch NumberFormatException _ -1))]
              (if (< -1 choice-ndx (count choices))
                (second (nth choices choice-ndx))
                (recur current-name-endpoint-map))))))
      (finally
        (.start (Thread. tear-down-mdns))))))

(defn socket
  [host port]
  {:pre [(string? host) (number? port)]}
  (let [socket (doto (Socket. host port) (.setKeepAlive true))
        in     (io/reader socket)
        out    (io/writer socket)]
    {:socket socket :in in :out out}))

(defn close-socket
  [s]
  {:pre [(map? s)]}
  (.close (:socket s)))

(defn write
  [out js]
  (:pre [(instance? BufferedWriter out) (string? js)])
  (.write out js)
  (.write out (int 0)) ;; terminator
  (.flush out))

(defn read-messages
  [in response-promise opts]
  {:pre [(instance? BufferedReader in) (map? opts)]}
  (loop [sb (StringBuilder.) c (.read in)]
    (cond
      (= c -1) (do
                 (if-let [resp-promise @response-promise]
                   (deliver resp-promise :eof))
                 :eof)
      (= c 1) (do
                (print (str sb))
                (flush)
                (recur (StringBuilder.) (.read in)))
      (= c 0) (do
                (deliver @response-promise (str sb))
                (recur (StringBuilder.) (.read in)))
      :else (do
              (.append sb (char c))
              (recur sb (.read in))))))

(defn start-reading-messages
  "Starts a thread reading inbound messages."
  [repl-env opts]
  {:pre [(map? repl-env) (map? opts)]}
  (.start
    (Thread.
      #(try
        (let [rv (read-messages (:in @(:socket repl-env)) (:response-promise repl-env) opts)]
          (when (= :eof rv)
            (close-socket @(:socket repl-env))))
        (catch IOException e
          (when-not (.isClosed (:socket @(:socket repl-env)))
            (.printStackTrace e)))))))

(def not-conected-result
  {:status :error
   :value "Not connected."})

(defn esprit-eval
  "Evaluate a JavaScript string in the Espruino REPL process."
  [repl-env js]
  {:pre [(map? repl-env) (string? js)]}
  (let [{:keys [out]} @(:socket repl-env)
        response-promise (promise)]
    (if out
      (do
        (reset! (:response-promise repl-env) response-promise)
        (write out js)
        (let [response @response-promise]
          (if (= :eof response)
            not-conected-result
            (let [result (json/read-str response
                           :key-fn keyword)]
              (merge
                {:status (keyword (:status result))
                 :value  (:value result)}
                (when-let [raw-stacktrace (:stacktrace result)]
                  {:stacktrace raw-stacktrace}))))))
      not-conected-result)))

(defn load-javascript
  "Load a Closure JavaScript file into the Espruino REPL process."
  [repl-env provides url]
  (esprit-eval repl-env
    (str "goog.require('" (comp/munge (first provides)) "')")))

(defn- set-up-socket
  [repl-env opts address port]
  {:pre [(map? repl-env) (map? opts) (string? address) (number? port)]}
  (when-let [socket @(:socket repl-env)]
    (close-socket socket))
  (reset! (:socket repl-env)
    (socket address port))
  ;; Start dedicated thread to read messages from socket
  (start-reading-messages repl-env opts))

(defn tear-down
  [repl-env]
  (when-let [socket @(:socket repl-env)]
    (close-socket socket)))

(defn setup
  [repl-env opts]
  {:pre [(map? repl-env) (map? opts)]}
  (try
    (let [_ (set-logging-level "javax.jmdns" java.util.logging.Level/OFF)
          [bonjour-name endpoint] (if-let [endpoint-address (:endpoint-address (:options repl-env))]
                                    ["Esprit ESP32 WROVER" {:address endpoint-address :port 53001}]
                                    (discover-and-choose-device (:choose-first-discovered (:options repl-env)) opts))
          endpoint-address (:address endpoint)
          endpoint-port (:port endpoint)]
      (println (str "\nConnecting to " (bonjour-name->display-name bonjour-name) " ...\n"))
      (set-up-socket repl-env opts endpoint-address (dec endpoint-port))
      {})
    (catch Throwable t
      (tear-down repl-env)
      (throw t))))

(defrecord EspritEnv [response-promise bonjour-name webdav-mount-point socket options]
  repl/IReplEnvOptions
  (-repl-options [this]
    {:require-foreign true})
  repl/IJavaScriptEnv
  (-setup [repl-env opts]
    (setup repl-env opts))
  (-evaluate [repl-env _ _ js]
    (esprit-eval repl-env js))
  (-load [repl-env provides url]
    (load-javascript repl-env provides url))
  (-tear-down [repl-env]
    (tear-down repl-env)))

(defn repl-env*
  [options]
  {:pre [(or (nil? options) (map? options))]}
  (->EspritEnv (atom nil) (atom nil) (atom nil) (atom nil) (into {:endpoint-address
                                                                  (:endpoint-address env)
                                                                  :choose-first-discovered
                                                                  (:choose-first-discovered env)}
                                                                 options)))

(defn repl-env
  "Esprit REPL environment."
  [& {:as options}]
  (repl-env* options))

(defn -main
  "Launches the Esprit REPL."
  []
  (repl/repl (repl-env)))
