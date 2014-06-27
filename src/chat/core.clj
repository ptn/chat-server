(ns chat.core
  (:require [clojure.string :as str])
  (:import (java.io BufferedReader InputStreamReader OutputStreamWriter))
  ;; this is how you import code from contrib:
  (:use [server.socket :only [create-server]]))

(defn logged-in? [in-out->username in-out]
  (contains? @in-out->username in-out))

(defn cmd-login [in-out->username in-out username]
  (dosync
   ;; ensure protects the ref from modification from other transactions, so from
   ;; this point on, it is safe to read and then write without fearing that the
   ;; value may have changed in between.
   ;;
   ;; how to test this?
   (let [ensured (ensure in-out->username)]
     ;; running vals and then some traverses the map twice (or a data structure
     ;; of that same length)
     (when-not (some #{username} (vals ensured))
       (alter in-out->username assoc in-out username)))))

(def cmds {:login cmd-login})

(defn cmd-send-msg
  [in-out->username in-out msg]
  (println msg))

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

(defn chat-handler
  "Returns a closure that implements the chat protocol, closed over
  a map of a set of in out streams to usernames."
  ;; My intuition of refs so far is: things that are safe to mutate even from
  ;; different threads. I should read up more on them. How are they different
  ;; from atoms?
  [in-out->username]
  (let [in-out->username (ref in-out->username)]
    (fn [in out]
      (binding [*in* (BufferedReader. (InputStreamReader. in))
                *out* (OutputStreamWriter. out)]
        (loop []
          (let [[cmd args] (parse-cmd (str/trim (read-line)))]
            (if (or (logged-in? in-out->username #{in out})
                    (= cmd cmd-login))
              ;; what is the commands contract? what should they return -
              ;; a boolean indicating success/failure?
              (do (cmd in-out->username #{in out} args)
                  (recur))
              (do
                (println "Need to login!")
                (recur)))))))))

(defn chat-server [port]
  (create-server port (chat-handler {})))

;; (def srvr (chat-server 4555))
