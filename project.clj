(defproject chat "0.1.0-SNAPSHOT"
  :description "A simple chat server"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; so to use stuff in contrib you just reference them by name
                 ;; here in project.clj, cool.  See the note on how to import
                 ;; them in src/chat/core.clj
                 [server-socket "1.0.0"]]
  :main chat.core)
