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
  (jdbc/with-transaction [t-conn *db*]
    (let [timestamp (java.time.LocalDateTime/now)
          message   {:name "Bob"
                     :message "Hello, world!"
                     :timestamp timestamp}]
      (t/is (= 1 (db/save-message! t-conn message {:connection t-conn})))
      (let [queried-message (first (db/get-messages t-conn {}))]
        (t/is (= (dissoc message :timestamp)
                 (select-keys queried-message [:name :message])))
        (t/is (= (-> message :timestamp .toLocalDate)
                 (-> queried-message :timestamp .toLocalDate)))))))
