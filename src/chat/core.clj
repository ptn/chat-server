(ns chat.core
  (:require [clojure.string :as str]
            [chat.select :as select]
            [chat.protocol :as protocol])
  (:import [java.nio.channels SelectionKey]
           [java.nio ByteBuffer]
           [java.nio.charset Charset]))

(def buf-size 8096)

(defmulti select-handler
  (fn [key] (.readyOps key)))

(defmethod select-handler SelectionKey/OP_ACCEPT
  [key]
  ;; (.channel key) is the server socket which listens for new
  ;; connections
  (-> (.channel key)
      (.accept)
      (.configureBlocking false)
      (.register (.selector key) SelectionKey/OP_READ)
      (.attach {:rbuf (ByteBuffer/allocate buf-size)
                :wbuf (ByteBuffer/allocate buf-size)}))
  (println "Accepted connection"))

(defn- close-conn
  [key]
  (println "Lost connection from" (.channel key))
  (.cancel key)
  (.close (.channel key))
  (println "Closed" (.channel key)))

(defn- decode-buf
  [buf]
  (->> buf (.decode (Charset/defaultCharset)) .toString))

(defn- encode-to-buf
  [s]
  (.encode (Charset/defaultCharset) s))

;; for now assumes you get a full command in every read
(defmethod select-handler SelectionKey/OP_READ
  [key]
  (let [rbuf (-> (.attachment key) (:rbuf) (.clear))
        bytes-read (.read (.channel key) rbuf)]
    (if (= bytes-read -1)
      (close-conn key)
      (let [msg (decode-buf (.flip rbuf))
            response (protocol/exec-cmd msg)
            wbuf (-> (.attachment key) (:wbuf) .clear)]
        (.put wbuf (encode-to-buf response))
        (.flip wbuf)
        (.write (.channel key) wbuf)))))

;; stub
(defn chat-server
  [port]
  (select/server port))
