(ns coup.db.core-test
  "Entry database testing namespace, testing basic functions and providing functions for testing"
  (:require [coup.db.core :refer [*db*] :as db]
            [coup.db.test-util :as tcore]
            [coup.handler :refer [app]]
            [mount.core]
            [coup.db.test-data :as td]
            [clojure.test :refer [deftest is testing]]))

(tcore/basic-transaction-fixtures
 (mount.core/start #'coup.handler/app))

