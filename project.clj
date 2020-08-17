(defproject esprit "0.9.0"
  :description "ClojureScript on the ESP32 using Espruino"
  :url "https://github.com/mfikes/esprit"
  :source-paths ["src" "resources"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.764" :scope "provided"]
                 [com.github.rickyclarkson/jmdns "3.4.2-r353-1"]
                 [yogthos/config "1.1.7"]
                 [org.clojure/tools.cli "1.0.194"]])
