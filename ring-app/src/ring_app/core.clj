(ns ring-app.core
  (:require
    [clojure.pprint]
    [muuntaja.middleware :as muuntaja]
    [reitit.ring :as reitit]
    [ring.adapter.jetty :as jetty]
    [ring.util.http-response :as response]
    [ring.middleware.reload :as reload]))

(defn html-handler [request]
  (response/ok
   (str "<h1>The request map is:</h1>\n"
        "<pre style=\"word-wrap: break-word; white-space: pre-wrap;\">\n"
        (with-out-str (clojure.pprint/pprint request))
        "</pre>")))

(defn json-handler [request]
  (response/ok
   {:result (get-in request [:body-params])}))

(defn wrap-nocache [handler]
  (fn [request]
    (-> request handler (assoc-in [:headers "Pragma"] "no-cache"))))

(defn wrap-formats [handler]
  (-> handler (muuntaja/wrap-format)))

(def routes
  [["/" {:get html-handler
         :post html-handler}]
   ["/echo/:id"
    {:get (fn [request]
            (response/ok (str "<h1>the value of id id is: "
                              (-> request :path-params :id)
                              "</h1>")))}]
   ["/api"
    {:middleware [wrap-formats]}
    ["/multiply"
     {:post
      (fn [{{:keys [a b]} :body-params}]
        (response/ok {:result (* a b)}))}]]])

(def handler
  (reitit/routes
   (reitit/ring-handler
    (reitit/router routes))
   (reitit/create-resource-handler
    {:path "/"})
   (reitit/create-default-handler
    {:not-found
     (constantly (response/not-found "404 - Page not found"))
     :method-not-allowed
     (constantly (response/method-not-allowed "405 - Not allowed"))
     :not-acceptable
     (constantly (response/method-not-allowed "406 - Not acceptable"))})))

(defn -main []
  (jetty/run-jetty
   (-> #'handler
       wrap-nocache
       reload/wrap-reload)
   {:port 3000 :join? false}))
