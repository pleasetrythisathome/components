(ns synecdoche.om
  (:require
   [cljs.core.async :as async :refer [put! chan pub]]
   [goog.dom :as gdom]
   [om.core :as om :include-macros true]
   [om.dom :as dom]
   [om-tools.core :refer-macros [defcomponentk]]
   [sablono.core :as html :refer-macros [html]]
   [plumbing.core :refer-macros [defnk fnk <-]]
   [schema.core :as s]
   [quile.component :as component]))

(defprotocol IOmShared
  (shared [_]))

(defrecord OmShared [props]
  IOmShared
  (shared [_]
    props))

(defn new-shared [& {:as opts}]
  (-> opts
      (->OmShared)
      (component/using [])))

(defrecord OmRoot [target view state cursor tx-pub-chan]
  component/Lifecycle
  (start [this]
    (let [tx-notify-chan (chan)
          tx-pub-chan (pub tx-notify-chan :tag)
          cursor (atom nil)
          shared (->> this
                      vals
                      (filter #(satisfies? IOmShared %))
                      (map shared)
                      (apply merge {}))]
      (om/root (fn [data owner]
                 (reset! cursor data)
                 (reify
                   om/IRender
                   (render [_]
                     (view data))))
               state
               {:target target
                :shared (merge shared
                               {:system this
                                :tx-pub-chan tx-pub-chan})
                :tx-listen (fn [tx-data root-cursor]
                             (put! tx-notify-chan tx-data))})
      (assoc this
        :tx-pub-chan tx-pub-chan
        :cursor cursor)))
  (stop [this]
    (om/detach-root target)
    this))

(def new-om-root-schema
  {:target s/Str
   :state {s/Any s/Any}
   :view (s/make-fn-schema s/Any s/Any)})

(defn new-om-root
  [& {:as opts}]
  (-> opts
      (->> (merge {})
           (s/validate new-om-root-schema))
      (update :target gdom/getElement)
      (map->OmRoot)
      (component/using [])))
