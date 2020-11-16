(ns guestbook.core
 (:require
  [ajax.core :as ajax]
  [clojure.string :as string]
  [guestbook.validation :as validation]
  [reagent.core :as r]
  [re-frame.core :as rf]
  [reagent.dom :as dom]))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _]
    {:db {:messages/loading? true}}))

(rf/reg-sub
  :messages/loading?
  (fn [db _]
   (:messages/loading? db)))

(rf/reg-event-db
  :messages/set
  (fn [db [_ messages]]
    (-> db
        (assoc :messages/loading? false
               :messages/list messages))))

(rf/reg-sub
  :messages/list
  (fn [db _]
    (:messages/list db [])))

(rf/reg-event-db
  :message/add
  (fn [db [_ message]]
    (update db :messages/list conj message)))

(defn get-messages []
  (ajax/GET
   "api/messages"
   {:headers {"Accept" "application/transit+json"}
    :handler #(rf/dispatch [:messages/set (:messages %)])}))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(defn send-message! [fields errors]
  (if-let [validation-errors (validation/validate-message @fields)]
    (reset! errors validation-errors)
    (ajax/POST
      "api/message"
      {:format :json
       :headers
       {"Accept" "application/transit+json"
        "x-csrf-token" (.-value (.getElementById js/document "token"))}
       :params @fields
       :handler
       #(do
          (.log js/console (str "response:" %))
          (rf/dispatch [:message/add (assoc @fields :timestamp (js/Date.))])
          (reset! fields nil)
          (reset! errors nil))
       :error-handler
       #(do
          (.error js/console (str "error:" %))
          (reset! errors (get-in % [:response :errors])))})))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn message-form [messages]
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn []
      [:div
       [:p "Name: " (:name @fields)]
       [:p "Message: " (:message @fields)]
       [:p "Errors: " (str @errors)]
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [errors-component errors :name]
        [:input.input
         {:type :text
          :name :name
          :on-change #(swap! fields assoc :name (-> % .-target .-value))
          :value (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component errors :message]
        [:textarea.textarea
         {:name :message
          :value (:message @fields)
          :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
       [:input.button.is-primary
        {:type :submit
         :on-click #(send-message! fields errors)
         :value "comment"}]])))

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    (rf/dispatch [:app/initialize])
    (get-messages)
    (fn []
      (if @(rf/subscribe [:messages/loading?])
        [:div>div.row>div.span12>h3 "Loading Messages..."]
        [:div.content>div.columns.is-centered>div.column.is-two-thirds
         [:div.columns>div.column
          [:h3 "Messages"]
          [message-list messages]]
         [:div.columns>div.column
          [:h3 "New Message"]
          [message-form messages]]]))))

(dom/render
  [home]
  (.getElementById js/document "content"))

