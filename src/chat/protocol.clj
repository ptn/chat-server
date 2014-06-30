(ns chat.protocol
  (:require [clojure.string :as str])
  (:import [java.nio.charset Charset]))

;; deals with clients, which are maps composed of :channel, :rbuf and :wbuf
;;
;; commands perform the corresponding operation, perhaps modifying the
;; sockets-to-usernames map, and return the next command to run for
;; that client plus the new usernames map. If the command may be
;; assoc'd to the handlers map, then it only takes the usernames map
;; and the client as parameters, if it's something that requires user
;; input, then it also takes said input.
;;
;; also contains utilities for reading from and writing to sockets,
;; with buffers

(declare exec-cmd)
(declare require-login)

(defn- encode-to-buf
  [s]
  (.encode (Charset/defaultCharset) s))

;; for now assumes you get a full command in every read
(defn- read-client
  [client]
  (.clear (:rbuf client))
  (let [bytes-read (.read (:channel client) (:rbuf client))]
    (when-not (= bytes-read -1)
      (->> (.flip (:rbuf client))
           (.decode (Charset/defaultCharset))
           .toString))))

(defn- write-to-client
  [client msg]
  (.clear (:wbuf client))
  (let [encoded (.encode (Charset/defaultCharset) (str msg "\n"))]
    (.put (:wbuf client) encoded)
    (.flip (:wbuf client))
    (.write (:channel client) (:wbuf client))))

(defn cmd-login
  [usernames client username]
  (if-not (some #{username} (vals usernames))
    (do
      (write-to-client client (str "Welcome " username "!"))
      [exec-cmd
       (assoc usernames (:channel client) username)])
    (do
      (write-to-client client "Username taken")
      [require-login usernames])))

(defn cmd-send-msg
  [usernames client msg]
  (write-to-client client msg)
  [exec-cmd usernames])

(def cmds {:login cmd-login})

(defn parse-cmd
  [line]
  (let [[cmd-name args] (str/split line #" ")]
    (if (= (first cmd-name) \/)
      (let [cmd-name (-> cmd-name (subs 1) str/lower-case)
            cmd (or ((keyword cmd-name) cmds)
                    cmd-send-msg)
            args (if (= cmd cmd-send-msg) line args)]
        [cmd args])
      [cmd-send-msg line])))

(defn exec-cmd
  [usernames client]
  (when-let [msg (read-client client)]
    (let [[cmd args] (parse-cmd (str/trim msg))]
      (if (= cmd cmd-login)
        (do
          (write-to-client client "Already logged in!")
          [exec-cmd usernames])
        (cmd usernames client args)))))

(defn require-login
  [usernames client]
  (when-let [msg (read-client client)]
    (let [[cmd args] (parse-cmd (str/trim msg))]
      (if (= cmd cmd-login)
        (cmd-login usernames client args)
        (do
          (write-to-client client "Need to login!")
          [require-login usernames])))))
