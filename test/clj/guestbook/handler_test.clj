(ns guestbook.handler-test
  (:require
   [clojure.test :as t]
   [ring.mock.request :as request]
   [guestbook.handler :as handler]
   [guestbook.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(t/use-fixtures
  :once
  (fn [f]
    (mount/start #'guestbook.config/env
                 #'guestbook.handler/app-routes)
    (f)))

(t/deftest test-app
  (t/testing "main route"
    (let [response ((handler/app) (request/request :get "/"))]
      (t/is (= 200 (:status response)))))

  (t/testing "not-found route"
    (let [response ((handler/app) (request/request :get "/invalid"))]
      (t/is (= 404 (:status response))))))
