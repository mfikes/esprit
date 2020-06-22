(ns esprit.make-rom
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]))

(defn -main []
  (let [main-js    (-> (str (slurp (io/resource "esprit/init.js")) (slurp "out/main.js"))
                     (string/replace "a: {" "{")
                     (string/replace "a:for" "for")
                     (string/replace "goog.uri.utils.splitRe_=/^(?:([^:/?#.]+):)?(?:\\/\\/(?:([^/?#]*)@)?([^/#?]*?)(?::([0-9]+))?(?=[/#?]|$))?([^?#]+)?(?:\\?([^#]*))?(?:#([\\s\\S]*))?$/" "goog.uri.utils.splitRe_=null")
                     (string/replace "= /^(?:([^:/?#.]+):)?(?:\\/\\/(?:([^/?#]*)@)?([^/#?]*?)(?::([0-9]+))?(?=[/#?]|$))?([^?#]+)?(?:\\?([^#]*))?(?:#([\\s\\S]*))?$/" "= null")
                     #_(string/replace "of(" "of2(")
                     #_(string/replace "of)" "of2)")
                     #_(string/replace "var of" "var of2")
                     #_(string/replace "of.prototype" "of2.prototype"))
        main-js    (str "Array.prototype.concat = [].concat;\n" main-js)
        _          (spit "out/main-modified.js" main-js)
        size       (count main-js)
        size-bytes (byte-array (map #(bit-and (bit-shift-right size %) 0xff) [0 8 16 24]))]
    (with-open [os (io/output-stream "out/main.bin")]
      (.write os size-bytes)
      (.write os (byte-array [0xff 0xff 0xff 0xff]))
      (.write os (.getBytes ".bootcde"))
      (.write os (.getBytes main-js)))
    (println "ROM created; you can flash it to your ESP32 by executing the following:")
    (println "esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x2C0000 out/main.bin")))
