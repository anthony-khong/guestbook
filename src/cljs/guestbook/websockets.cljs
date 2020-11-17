(ns guestbook.websockets
  (:require [cljs.reader :as edn]))

(defonce channel (atom nil))

(defn connect! [url receive-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "Connected!")
      (set!
       (.-onmessage chan)
       #(->> % .-data edn/read-string receive-handler))
      (reset! channel chan))
    (throw (ex-info "Websocket connection failed!" {:url url}))))

(defn send-message! [message]
  (if-let [chan @channel]
    (.send chan (pr-str message))
    (throw (ex-info "Couldn't send message, channel isn't open!"
                    {:message message}))))
