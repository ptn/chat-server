(ns chat.core
  (:import (java.io BufferedReader InputStreamReader OutputStreamWriter))
  ;; this is how you import code from contrib:
  (:use [server.socket :only [create-server]]))

(defn echo-handler
  "Echo back to a client every byte we receive from it."
  [in out]
  (binding [*in* (BufferedReader. (InputStreamReader. in))
            *out* (OutputStreamWriter. out)]
    (loop []
      (let [bytes (read-line)]
        (println bytes)
        (flush))
      (recur))))

(def echo-server (create-server 4555 echo-handler))
