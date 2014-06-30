(ns chat.select
  (:import [java.net InetSocketAddress]
           [java.nio.channels ServerSocketChannel Selector SelectionKey]))


(defn select
  [selector]
  (.clear (.selectedKeys selector))
  (.select selector)
  (.selectedKeys selector))

(defn server
  [port]
  (let [listener (-> (ServerSocketChannel/open)
                     (.configureBlocking false)
                     (.bind (InetSocketAddress. port)))
        selector (Selector/open)]
    (.register listener selector SelectionKey/OP_ACCEPT)
    {:listener listener :selector selector}))
