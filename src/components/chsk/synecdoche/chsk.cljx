(ns synecdoche.chsk
  #+clj
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [plumbing.core :refer :all :exclude [update]]
   [taoensso.sente :as sente]
   [schema.core :as s]
   [clojure.test :refer (function?)])
  #+cljs
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]])
  #+cljs
  (:require
   [quile.component :as component]
   [plumbing.core :refer-macros [defnk fnk <-]]
   [schema.core :as s]
   [cljs.core.async :as async :refer (<! >! put! chan dropping-buffer)]
   [synecdoche.utils.async :refer [control-loop]]
   [taoensso.sente  :as sente]))

#+clj
(defn uuid [] (str (java.util.UUID/randomUUID)))

(defprotocol ChannelSocketHandler
  (handler [_  chsk]))

#+clj
(defrecord ChannelSockets
    [chsk-handler user-id-fn ring-ajax-post ring-ajax-get-or-ws-handshake ch-chsk chsk-send! connected-uids router]
  component/Lifecycle
  (start [this]
    (let [{:keys [ch-recv
                  send-fn
                  ajax-post-fn
                  ajax-get-or-ws-handshake-fn
                  connected-uids] :as chsk} (sente/make-channel-socket! {:user-id-fn user-id-fn})]
      (assert chsk-handler "sente module requires the existence of a component that satisfies ChannelSocketHandler to start")
      (assoc this
        :ring-ajax-post ajax-post-fn
        :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn
        :ch-chsk ch-recv
        :chsk-send! send-fn
        :connected-uids connected-uids
        :router (atom (sente/start-chsk-router! ch-recv (handler chsk-handler chsk))))))
  (stop [this]
    (if-let [stop-f (some-> router deref)]
      (assoc this :router (stop-f))
      this)))

#+cljs
(defrecord ChannelSockets
    [chsk-handler context type chsk ch-chsk chsk-send! chsk-state]
  component/Lifecycle
  (start [this]
    (let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket! context {:type type})
          send-queue (chan)
          control-chan (control-loop send-fn send-queue)]
      ;; don't send until chsk is open
      (put! control-chan :pause)
      (add-watch state :conn
                 (fn [_key _ref old-value {:keys [open? destroyed?] :as v}]
                   (put! control-chan (if-not destroyed?
                                        (if open?
                                          :play
                                          :pause)
                                        :kill))))
      (assoc this
        :chsk chsk
        :ch-chsk ch-recv
        :chsk-send! #(put! send-queue %)
        :chsk-state state
        :router (atom (sente/start-chsk-router! ch-recv (handler chsk-handler chsk))))))
  (stop [this]
    this))

#+clj
(def new-channel-sockets-schema
  {:user-id-fn (s/pred function?)})

#+cljs
(def new-channel-sockets-schema
  {:context s/Str
   :type (s/enum :auto :ajax :ws)})

(defn new-channel-sockets
  [& {:as opts}]
  (->> opts
       (merge #+clj {:user-id-fn (fn [req] (uuid))}
              #+cljs {:context "/chsk"
                      :type :auto})
       (s/validate new-channel-sockets-schema)
       map->ChannelSockets
       (<- (component/using [:chsk-handler]))))
