(ns chat.core
  (:require [clojure.string :as str]
            [chat.select :as select]
            [chat.protocol :as protocol])
  (:import [java.nio.channels SelectionKey]
           [java.nio ByteBuffer]))

(def buf-size 8096)

(defn- close-conn
  [key]
  (println "Lost connection from" (.channel key))
  (.cancel key)
  (.close (.channel key))
  (println "Closed" (.channel key)))

(defn- accept-client
  [key]
  (let [new-client (.accept (.channel key))]
    (-> new-client
        (.configureBlocking false)
        (.register (.selector key) SelectionKey/OP_READ)
        (.attach {:rbuf (ByteBuffer/allocate buf-size)
                  :wbuf (ByteBuffer/allocate buf-size)}))
    (println "Accepted connection from" new-client)
    new-client))

(defn- run-handler-for
  "Runs the handler for the given key and returns all the info
  necessary for next-handlers-and-usernames to update the handlers and
  usernames maps.

  The new handler plus the new usernames map are taken from the return
  value of the handler for the given key, and the channel is deduced
  from the specific key."
  [k handlers usernames]
  (cond
   (= SelectionKey/OP_ACCEPT (.readyOps k))
   [protocol/require-login usernames (accept-client k)]

   (= SelectionKey/OP_READ (.readyOps k))
   (let [handler (get handlers (.channel k))
         [new-handler new-usernames] (handler usernames
                                              {:channel (.channel k)
                                               :rbuf (:rbuf (.attachment k))
                                               :wbuf (:wbuf (.attachment k))})]
     [new-handler new-usernames (.channel k)])))

(defn- next-handlers-and-usernames
  "Update the handlers and usernames maps with the handler, username
  and channel that correspond to the given key."
  [[handlers usernames] k]
  (let [[handler usernames' ch] (run-handler-for k handlers usernames)]
    ;; handler is overloaded, it means both the next handler and
    ;; success/failure of the current handler
    (if handler
      [(assoc handlers ch handler) usernames']
      (do
        (close-conn k)
        [(dissoc handlers (.channel k))
         (dissoc usernames (.channel k))]))))

(defn chat-server
  [port]
  (let [server (select/server port)]
    ;; handlers maps key channels to closures that handle them
    ;; usernames maps channels to usernames
    ;;
    ;; maybe it'll be a good idea to wrap these in a type, which would
    ;; let me have a separate set of usernames and the reverse mapping
    ;; of usernames to sockets while ensuring consistency to help with
    ;; efficiency
    (loop [handlers {}
           usernames {}]
      (let [keys (select/select (:selector server))
            [handlers' usernames'] (reduce next-handlers-and-usernames
                                           [handlers usernames]
                                           keys)]
        (recur handlers' usernames')))))
