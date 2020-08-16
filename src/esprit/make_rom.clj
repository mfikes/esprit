(ns esprit.make-rom
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]))

(defn -main []
  (let [main-js    (-> (slurp "out/main.js")
                       ; Random things that break
                       (string/replace "/[\\\\\"\\b\\f\\n\\r\\t]/g" "/[\\\\\"\\f\\n\\r\\t]/g")
                       (string/replace "goog.NONCE_PATTERN_=/^[\\w+/_-]+[=]{0,2}$/" "goog.NONCE_PATTERN_=null")
                       (string/replace "/^((https:)?\\/\\/[0-9a-z.:[\\]-]+\\/|\\/[^/\\\\]|[^:/\\\\%]+\\/|[^:/\\\\%]*[?#]|about:blank#)/i" "null")
                       (string/replace "/^(?:(?:https?|mailto|ftp):|[^:/?#]*(?:[/?#]|$))/i" "null")
                       (string/replace "a: {" "{")
                       (string/replace "a:for" "for")
                       (string/replace "goog.uri.utils.splitRe_=/^(?:([^:/?#.]+):)?(?:\\/\\/(?:([^/?#]*)@)?([^/#?]*?)(?::([0-9]+))?(?=[/#?]|$))?([^?#]+)?(?:\\?([^#]*))?(?:#([\\s\\S]*))?$/" "goog.uri.utils.splitRe_=null")
                       (string/replace "= /^(?:([^:/?#.]+):)?(?:\\/\\/(?:([^/?#]*)@)?([^/#?]*?)(?::([0-9]+))?(?=[/#?]|$))?([^?#]+)?(?:\\?([^#]*))?(?:#([\\s\\S]*))?$/" "= null"))
        main-js    (str "Array.prototype.concat = [].concat;\n" main-js)
        _          (spit "out/main-modified.js" main-js)
        bytes      (.getBytes main-js "UTF-8")
        size       (count bytes)
        size-bytes (byte-array (map #(bit-and (bit-shift-right size %) 0xff) [0 8 16 24]))]
    (with-open [os (io/output-stream "out/main.bin")]
      (.write os size-bytes)
      (.write os (.getBytes ".bootcde"))
      (.write os (byte-array [0x00 0x00 0x00 0x00]))
      (.write os (byte-array [0x00 0x00 0x00 0x00]))
      (.write os (byte-array [0x00 0x00 0x00 0x00]))
      (.write os (byte-array [0x00 0x00 0x00 0x00]))
      (.write os (byte-array [0x00 0x00 0x00 0x00]))
      (.write os bytes))
    (println "ROM created; you can flash it to your ESP32 by executing the following:")
    (println "esptool.py --port /dev/cu.SLAB_USBtoUART --baud 2000000 write_flash 0x320000 out/main.bin")))
