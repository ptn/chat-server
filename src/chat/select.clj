(ns chat.select
  (:import [java.net InetSocketAddress]
           [java.nio.channels ServerSocketChannel Selector SelectionKey]))

;; A select-based, non-blocking server.
;;
;; Run one as such:
;;
;; (let [server (server PORT)]
;;   (select (:selector server) handler))
;;
;; handler is a function that takes a selected key as returned by
;; (.selectedKeys selector) and processes it; you probably want to
;; check its readyOps field to determine why it was selected and
;; proceed accordingly.
;;
;; The run function is provided to repeatedly perform calls to select
;; in a loop:
;;
;; (let [server (server PORT)]
;;   (run  server handler)) ;; calls select forever


(defn select
  [selector handler]
  (.select selector)
  (let [ks (.selectedKeys selector)]
    ;; doseq works but map doesn't, why? -- because map is lazy,
    ;; Clojure doesn't actually do any work unless you catch the
    ;; result of map and access it later.
    (doseq [k ks]
      (handler k))
    (.clear ks)))

(defn server
  [port]
  (let [listener (-> (ServerSocketChannel/open)
                     (.configureBlocking false)
                     (.bind (InetSocketAddress. port)))
        selector (Selector/open)]
    (.register listener selector SelectionKey/OP_ACCEPT)
    {:listener listener :selector selector}))

(defn run
  [server handler]
  (loop []
    (select (:selector server) handler)
    (recur)))
