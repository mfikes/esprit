(ns esprit.flash
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.io File]
           [java.lang ProcessBuilder]))

(def memory-layout
  {"bootloader.bin" 0x1000
   "partitions_espruino.bin" 0x8000
   "espruino_esp32.bin" 0x10000
   "main.bin" 0x320000})

(defn run-and-print [& cmd-and-args]
  (.. (doto (ProcessBuilder. cmd-and-args)
        (.inheritIO))
      (start)
      (waitFor)))

(defn erase
  "Erase entire ESP32 flash"
  ([]
   (run-and-print "esptool.py" "erase_flash"))
  ([port]
   (run-and-print "esptool.py" "--port" port "erase_flash")))

(defn flash
  "Flash `bin` to the `addr` location"
  ([bin addr]
   (run-and-print "esptool.py" "--baud" "2000000" "write_flash" (str addr) bin))
  ([bin addr port]
   (run-and-print "esptool.py" "--baud" "2000000" "--port" port "write_flash" (str addr) bin)))

(defn resource-to-tmp
  "Copy a file from the classpath `resource` to a temp file and return the File obj"
  [resource]
  (with-open [in (io/input-stream (io/resource resource))]
    (let  [temp-file (File/createTempFile resource nil)]
      (io/copy in temp-file)
      temp-file)))

(defn bootstrap
  "Flash the Espruino core, partition table, and bootloader to the ESP32"
  ([]
   (doseq [[file addr] memory-layout
           :when (not (= file "main.bin"))]
     (flash (str (resource-to-tmp file)) addr)))
  ([port]
   (doseq [[file addr] memory-layout
           :when (not (= file "main.bin"))]
     (flash (str (resource-to-tmp file)) addr port))))

(def cli-options
  ;; An option with a required argument
  [["-b" "--bootstrap"]
   ["-e" "--erase"]
   ["-p" "--port PORT" "ESP32's Serial Port"]
   ["-h" "--help"]
   ["-f" "--flash BINARY" "JS binary to flash to ESP32"
    :validate [#(.exists (io/file %)) "Binary does not exists"]]])

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (cond
      (:errors opts) (run! println (:errors opts))
      (:erase (:options opts)) (if-let [port (:port (:options opts))]
                                 (erase port)
                                 (erase))
      (:bootstrap (:options opts)) (if-let [port (:port (:options opts))]
                                     (bootstrap port)
                                     (bootstrap))
      (:help (:options opts)) (println (:summary opts))
      (:flash (:options opts)) (if-let [port (:port (:options opts))]
                                 (flash (:flash (:options opts)) (get memory-layout "main.bin") port)
                                 (flash (:flash (:options opts)) (get memory-layout "main.bin"))))))
