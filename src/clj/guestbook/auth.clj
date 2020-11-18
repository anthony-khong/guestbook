(ns guestbook.auth
  [:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [guestbook.db.core :as db]])

(defn create-user! [login password]
  ; with clojure.java.jdbc
  ; (jdbc/with-db-transaction [t-conn db/*db*]
  (jdbc/with-transaction [t-conn db/*db*]
    (if-not (empty? (db/get-user-for-auth* t-conn {:login login}))
      (throw (ex-info "User already exists!"
                      {:guestbook/error-id ::dubplicate-user
                       :error "User already exists!"}))
      (db/create-user!* t-conn
                        {:login login
                         :password (hashers/derive password)}))))

(defn authenticate-user [login password]
  (let [{hashed :password :as user} (db/get-user-for-auth* {:login login})]
    (when (hashers/check password hashed)
      (dissoc user :password))))


(comment
  (create-user! "testuser" "testpass")
  (authenticate-user "testuser" "testpass")
  (authenticate-user "testuser" "wrongpass"))
