(ns chat.core
  (:require [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader OutputStreamWriter))
  ;; this is how you import code from contrib:
  (:use [server.socket :only [create-server]]))

(defn cmd-login [username] username)

(def cmds {:login cmd-login})

(defn cmd-send-msg
  [msg]
  ;; will this work with the binding form even if it is defined outside of it
  ;; but still invoked from the inside?
  (println msg))

(defn parse-cmd
  "Split a line received from a client into its first word and the rest.
  The first word acts as the command to run, and the rest as its parameters."
  [line]
  (let [[cmd-name args] (str/split line #" ")]
    (if (= (first cmd-name) \/)
      (let [cmd-name (-> cmd-name (subs 1) str/lower-case)
            cmd (or ((keyword cmd-name) cmds)
                    ;; if the first word starts with a / but is not a command,
                    ;; send the whole line as a message to all users.
                    cmd-send-msg)
            args (if (= cmd cmd-send-msg) line args)]
        [cmd args])
      [cmd-send-msg line])))

(defn chat-handler
  "Implements the chat protocol."
  [in out]
  (binding [*in* (BufferedReader. (InputStreamReader. in))
            *out* (OutputStreamWriter. out)]
    (loop [username ""]
      (let [[cmd args] (parse-cmd (str/trim (read-line)))]
        (if (= username "")
          (if (= cmd cmd-login)
            ;; TODO: don't allow repeated usernames
            (let [username (cmd args)]
              (println (format "Logged in as %s" username))
              (recur username))
            (do
              (println "Need to login!")
              (recur username)))
          ;; TODO: implement other commands once user is already
          ;; logged-in
          (do
            (println (format "Already logged in as %s" username))
            (recur username)))))))

;; (def srvr (create-server 4555 chat-handler))
