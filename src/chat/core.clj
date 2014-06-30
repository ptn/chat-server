(ns chat.core
  (:require [clojure.string :as str]
            [chat.select :as select]
            [chat.protocol :as protocol])
  (:import [java.nio.channels SelectionKey]
           [java.nio ByteBuffer]
           [java.nio.charset Charset]))

(def buf-size 8096)

(defn accept-client
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
(defn- read-client
  [key]
  (let [rbuf (-> (.attachment key) (:rbuf) (.clear))
        bytes-read (.read (.channel key) rbuf)]
    (if (= bytes-read -1)
      (close-conn key)
      (let [msg (decode-buf (.flip rbuf))
            wbuf (-> (.attachment key) (:wbuf) .clear)]
        (.put wbuf (encode-to-buf msg))
        (.flip wbuf)
        (.write (.channel key) wbuf)))))

(defn chat-server
  [port]
  (let [server (select/server port)]
    (loop [clients {}]
      (let [keys (select/select (:selector server))]
        (doseq [k keys]
          (cond
           (= SelectionKey/OP_ACCEPT (.readyOps k)) (accept-client k)
           (= SelectionKey/OP_READ   (.readyOps k)) (read-client k)))
        (recur clients)))))
