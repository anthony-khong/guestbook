(ns guestbook.routes.home
  (:require
   [clojure.java.io :as io]
   ;[guestbook.db.core :as db]
   [guestbook.layout :as layout]
   [guestbook.middleware :as middleware]
   ;[ring.util.http-response :as response]))
   [ring.util.response]))

(defn home-page [request]
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

