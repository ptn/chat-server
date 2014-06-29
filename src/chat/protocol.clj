(ns chat.protocol
  (:require [clojure.string :as str]))

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

(defn exec-cmd [x] x)
