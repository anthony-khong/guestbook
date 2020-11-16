(ns guestbook.routes.services
  (:require
   [guestbook.messages :as messages]
   [guestbook.middleware :as middleware]
   [ring.util.http-response :as response]))

(defn save-message! [{:keys [params]}]
  (try
    (messages/save-message! params)
    (response/ok {:status :ok})
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
   {:middleware [middleware/wrap-formats]}
   ["/messages"
    {:get (fn [_] (response/ok (messages/message-list)))}]
   ["/message"
    {:post save-message!}]])
