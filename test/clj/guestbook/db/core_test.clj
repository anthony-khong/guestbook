(ns guestbook.db.core-test
  (:require
   [clojure.test :as t]
   [guestbook.config :refer [env]]
   [guestbook.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [mount.core :as mount]
   [next.jdbc :as jdbc]))

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
    (t/is (= 1 (db/create-user!
                t-conn
                {:id         "1"
                 :first_name "Sam"
                 :last_name  "Smith"
                 :email      "sam.smith@example.com"
                 :pass       "pass"}
                {})))
    (t/is (= {:id         "1"
              :first_name "Sam"
              :last_name  "Smith"
              :email      "sam.smith@example.com"
              :pass       "pass"
              :admin      nil
              :last_login nil
              :is_active  nil}
             (db/get-user t-conn {:id "1"} {})))))
