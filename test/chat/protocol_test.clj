(ns chat.protocol-test
  (:require [clojure.test :refer :all]
            [chat.protocol :refer :all]))

(deftest test-parse-cmd
  (is (= (parse-cmd "hi")
         [cmd-send-msg "hi"]))
  (is (= (parse-cmd "/hi")
         [cmd-send-msg "/hi"]))
  (is (= (parse-cmd "/login hi")
         [cmd-login "hi"]))
  ;; "/nope" is not a command, so send the full string to all connected
  ;; users
  (is (= (parse-cmd "/nope nope")
         [cmd-send-msg "/nope nope"])))
