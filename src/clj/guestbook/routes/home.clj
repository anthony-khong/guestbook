(ns guestbook.routes.home
  (:require
   [guestbook.db.core :as db]
   [guestbook.layout :as layout]
   [guestbook.middleware :as middleware]))

(defn home-page [{:keys [flash] :as request}]
  (layout/render
   request
   "cljs_home.html"
   (merge {:messages (db/get-messages)}
          (select-keys flash [:name :message :errors]))))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

