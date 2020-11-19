(ns guestbook.routes.services
  (:require
   [guestbook.auth :as auth]
   [guestbook.messages :as messages]
   ;[guestbook.middleware :as middleware]
   [guestbook.middleware.formats :as formats]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :as response]
   [spec-tools.data-spec :as data-spec]))

(require '[clojure.pprint :refer [pprint]])

(defn save-message! [request]
  (println (:session request))
  (try
    (->> (messages/save-message!
          (-> request :session :identity)
          (:body-params request))
         (assoc {:status :ok} :post)
         response/ok)
    (catch Exception ex
      (let [{id     :guestbook/error-id
             errors :errors} (ex-data ex)]
        (case id
          :validation
          (response/bad-request {:errors errors})
          (response/internal-server-error
           {:errors {:server-error ["Failed to save message!"]}}))))))

(defn service-routes []
  ["/api"
   {:middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body]
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart params
                 multipart/multipart-middleware]
    :muuntaja formats/instance
    :coercion spec-coercion/coercion
    :swagger {:id ::api}}
   ["" {:no-doc true}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
   ["/login"
    {:post {:parameters {:body {:login string? :password string?}}
            :responses {200 {:body {:identity {:login string?}}}
                        401 {:body {:message string?}}}
            :handler
            (fn [request]
              (let [login (-> request :body-params :login)
                    password (-> request :body-params :password)
                    session (-> request :session)]
                (if-some [user (auth/authenticate-user login password)]
                  (-> (response/ok {:identity user})
                      (assoc :session (assoc session :identity user)))
                  (response/unauthorized
                    {:message "Incorrect login or password"}))))}}]
   ["/logout"
    {:post {:responses {200 {:body {:message string?}}}
            :handler (fn [_] (response/ok {:message "Logout successful."}))}}]
   ["/register"
    {:post {:parameters {:body {:login string? :password string? :confirm string?}}
            :responses {200 {:body {:message string?}}
                        400 {:body {:message string?}}
                        409 {:body {:message string?}}}
            :handler
            (fn [{{{:keys [login password confirm]} :body} :parameters}]
              (if-not (= password confirm)
                (response/bad-request {:message "Password and confirm do not match."})
                (try
                  (auth/create-user! login password)
                  (response/ok {:message "User registration successful. Please log in."})
                  (catch clojure.lang.ExceptionInfo e
                    (if (= (:guestbook/error-id (ex-data e))
                           ::auth/dubplicate-user)
                      (response/conflict
                        {:message "Registration failed! User login alread exists!"})
                      (throw e))))))}}]
   ["/session"
    {:get {:responses {200 {:body {:session {:identity (data-spec/maybe {:login string?})}}}}
           :handler
           (fn [request]
             (pprint (:session request))
             (response/ok {:session
                           {:identity
                            (not-empty
                              (select-keys {} [:login :created-at]))}}))}}]
   ["/messages"
    {:get
     {:responses
      {200 {:body {:messages [{:id pos-int?
                               :name string?
                               :message string?}]}}}
      :handler
      (fn [_] (response/ok (messages/message-list)))}}]
   ["/message"
    {:post
     {:parameters
      {:body {:name string? :message string?}}

      :responses
      {200 {:body map?}
       500 {:errors map?}}

      :handler
      save-message!}}]])
