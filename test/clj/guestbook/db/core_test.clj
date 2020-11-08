(ns guestbook.db.core-test
  (:require
   [guestbook.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :as t]
   [next.jdbc :as jdbc]
   [guestbook.config :refer [env]]
   [mount.core :as mount]))

(t/use-fixtures
  :once
  (fn [f]
    (mount/start
     #'guestbook.config/env
     #'guestbook.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(t/deftest test-users
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [message {:name "Bob" :message "Hello, world!"}]
      (t/is (= 1 (db/save-message! t-conn message {:connection t-conn})))
      (t/is (= (-> (db/get-messages t-conn {})
                   first
                   (select-keys [:name :message]))
               message)))))
