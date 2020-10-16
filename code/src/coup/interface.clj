(ns coup.interface
  (:require
    [nrepl.server :as nrepl]
    [coup.back :refer :all]))



;------------------------------------------------------------------
; Front-End Layer
;=-----------------------------------------------------------------

(defn game-loop []
  (if nil
    (recur)
    (println "done")))

(defn read-user []
  (do
    (print "enter username: ")
    (flush)
    (read-line)))

(defn run-cli []
  ; (init)
  ; (nrepl/start-server :port 7888)
  (println "started")
  ;(let [x (read-user)]
  ;  (signup-login x))
  (prn (select-all))
  ;(game-loop))
  #_(System/exit 0))

(comment
  ; Run t clean in terminal to clear database
  ; Run t dev in terminal to build and run


  ; Vim commands
  ; eval current form: cpp
  ; eval current element: cpie
  ; eval current file: ,e
  ; show docs for function: K
  ; (if that gives error, then do: ,K)

  (init)
  (insert-something)
  (doseq [m (select-all)]
    (println "bar: " (:bar m)))
  (select-something 2))

; https://github.com/seancorfield/next-jdbc/blob/develop/doc/getting-started.md
; https://cljdoc.org/d/seancorfield/next.jdbc/1.1.588/doc/getting-started/friendly-sql-functions
