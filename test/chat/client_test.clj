(ns chat.client-test
  (:import (java.io BufferedReader InputStreamReader OutputStreamWriter))
  (:require [clojure.test :refer :all]
            [chat.client :refer :all])
  (:use [server.socket :only [create-server]]))

(defn echo-handler
  [in out]
  (binding [*in* (BufferedReader. (InputStreamReader. in))
            *out* (OutputStreamWriter. out)]
    (loop []
      (let [bytes (read-line)]
        (println bytes)
        (flush))
      (recur))))

(def test-port 5444)

(deftest test-connect
  (let [echo-server (create-server test-port echo-handler)
        [socket conn] (connect "localhost" test-port)]
    (is (= (conn "testing") "testing"))
    (.close socket)
    (.close (:server-socket echo-server))))
