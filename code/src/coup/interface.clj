(ns coup.interface
  (:require
    [nrepl.server :as nrepl]
    [coup.back :refer :all]
    [coup.db :refer :all]
    [clojure.string :as str]))



;------------------------------------------------------------------
; Front-End Layer
;=-----------------------------------------------------------------

(defn parse-action
  "Gets first 2 space separated values from string"
  [line]
  (str/split line #" "))

(defn game-loop []
  (println "enter command")
  (let [line (read-line)]
    (if (not (= "exit" line))
      (do
        (receive-action (parse-action line))
        (recur))
      (println "done"))))

(defn read-user []
  (do
    (print "enter username: ")
    (flush)
    (read-line)))

(defn get-usernames
  "Reads in list of usernames from stdin, terminated by word 'done'"
  ([gathered-names]
   (let [new-username (read-user)]
     (if (= "done" new-username)
       gathered-names
       (get-usernames (conj gathered-names new-username)))))
  ([]
   (get-usernames '[])))

(defn print-instructions []
  (println "Welcome to Coup!")
  (println "At any point, type exit to quit."))


(defn run-cli []
  ; (init)
  ; (nrepl/start-server :port 7888)

  ; print instructions
  (print-instructions)

  ; read in users for game
  (let [usernames (get-usernames)]
    (println (init-game usernames)))
   ; (signup-login x))
  (prn (select-all "user"))
  (game-loop)
  (System/exit 0))


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
