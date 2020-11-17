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

(rf/reg-event-db
  :form/set-field
  [(rf/path :form/fields)]
  (fn [fields [_ id value]]
    (assoc fields id value)))

(rf/reg-event-db
  :form/clear-fields
  [(rf/path :form/fields)]
  (fn [_ _] {}))

(rf/reg-sub
  :form/fields
  (fn [db _] (:form/fields db)))

(rf/reg-sub
  :form/field
  :<- [:form/fields]
  (fn [fields [_ id]]
    (get fields id)))

(rf/reg-event-db
  :form/set-server-errors
  [(rf/path :form/server-errors)]
  (fn [_ [_ errors]] errors))

(rf/reg-sub
  :form/server-errors
  (fn [db _] (:form/server-errors db)))

;;Validation errors are reactively computed
(rf/reg-sub
  :form/validation-errors
  :<- [:form/fields]
  (fn [fields _]
    (validation/validate-message fields)))

(rf/reg-sub
  :form/validation-errors?
  :<- [:form/validation-errors]
  (fn [errors _] (seq errors)))

(rf/reg-sub
  :form/errors
  :<- [:form/validation-errors]
  :<- [:form/server-errors]
  (fn [[validation server] _]
     (merge validation server)))

(rf/reg-sub
  :form/error
  :<- [:form/errors]
  (fn [errors [_ id]]
    (get errors id)))

(rf/reg-event-fx
  :message/send!
  (fn [{:keys [db]} [_ fields]]
    (ajax/POST
      "api/message"
      {:format :json
       :headers
       {"Accept" "application/transit+json"
        "x-csrf-token" (.-value (.getElementById js/document "token"))}
       :params fields
       :handler
       #(rf/dispatch [:message/add (-> fields (assoc :timestamp (js/Date.)))])
       :error-handler
       #(rf/dispatch [:form/set-server-errors (get-in % [:response :errors])])})
    {:db (dissoc db :form/server-errors)}))

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
          (rf/dispatch [:message/add (-> @fields
                                         (assoc :timestamp (js/Date.))
                                         (update :name str "[CLIENT] "))])
          (reset! fields nil)
          (reset! errors nil))
       :error-handler
       #(do
          (.error js/console (str "error:" %))
          (reset! errors (get-in % [:response :errors])))})))

(defn errors-component [id]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (string/join error)]))

(defn message-form []
  (fn []
    [:div
     [:p "Name: " @(rf/subscribe [:form/field :name])]
     [:p "Message: " @(rf/subscribe [:form/field :message])]
     [:p "Errors: " @(rf/subscribe [:form/errors])]
     [errors-component :server-error]
     [:div.field
      [:label.label {:for :name} "Name"]
      [errors-component :name]
      [:input.input
       {:type :text
        :name :name
        :on-change #(rf/dispatch [:form/set-field :name (-> % .-target .-value)])
        :value @(rf/subscribe [:form/field :name])}]]
        ;:on-change #(swap! fields assoc :name (-> % .-target .-value))
        ;:value (:name @fields)}]]
     [:div.field
      [:label.label {:for :message} "Message"]
      [errors-component :message]
      [:textarea.textarea
       {:name :message
        ;:value (:message @fields)
        ;:on-change #(swap! fields assoc :message (-> % .-target .-value))
        :on-change #(rf/dispatch [:form/set-field :message (-> % .-target .-value)])
        :value @(rf/subscribe [:form/field :message])}]]
     [:input.button.is-primary
      {:type :submit
       :disabled @(rf/subscribe [:form/validation-errors?])
       :on-click #(rf/dispatch [:message/send! @(rf/subscribe [:form/fields])])
       :value "comment"}]]))

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    (fn []
      (if @(rf/subscribe [:messages/loading?])
        [:div>div.row>div.span12>h3 "Loading Messages..."]
        [:div.content>div.columns.is-centered>div.column.is-two-thirds
         [:div.columns>div.column
          [:h3 "Messages"]
          [message-list messages]]
         [:div.columns>div.column
          [:h3 "New Message"]
          [message-form]]]))))

(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (.log js/console "Mounting components...")
  (dom/render [#'home] (.getElementById js/document "content"))
  (.log js/console "Components mounted!"))

(defn init! []
  (.log js/console "Initialising app...")
  (rf/dispatch [:app/initialize])
  (get-messages)
  (mount-components)
  (.log js/console "guestbook.core evaluated!"))
