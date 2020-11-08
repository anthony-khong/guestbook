(ns guestbook.routes.home
  (:require
   [guestbook.db.core :as db]
   [guestbook.layout :as layout]
   [guestbook.middleware :as middleware]
   [ring.util.http-response :as response]
   [struct.core :as st]))

(defn home-page [{:keys [flash] :as request}]
  (layout/render
   request
   "home.html"
   (merge {:messages (db/get-messages)}
          (select-keys flash [:name :message :errors]))))

(defn about-page [request]
  (layout/render request "about.html"))

(def message-schema
  [[:name st/required st/string]
   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(< 9 (count %))}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))

(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (-> (response/found "/")
        (assoc :flash (assoc params :errors errors)))
    (do
      (db/save-message! params)
      (response/found "/"))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/message" {:post save-message!}]
   ["/about" {:get about-page}]])

