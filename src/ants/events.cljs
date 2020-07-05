(ns ants.events
  (:require
   [ants.config :as config]
   [ants.movement :as movement]
   [clojure.set :as set]
   [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(defn rename-key [m k-old k-new]
  (set/rename-keys m {k-old k-new}))

(defn move-ant [ants old-coordinate new-coordinate new-facing]
  (-> ants
      (rename-key old-coordinate new-coordinate)
      (update new-coordinate assoc :facing new-facing)))

(re-frame/reg-event-db
 :move
 (fn [db [_ old-coordinate new-coordinate new-facing]]
   (update db :ants move-ant old-coordinate new-coordinate new-facing)))

(re-frame/reg-event-db
 :rotate
 (fn [db [_ coordinate new-facing]]
   (update-in db [:ants coordinate] assoc :facing new-facing)))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(defn println-pass [o]
  (println :ant-options-> o)
  o)

(re-frame/reg-event-fx
 :tick
 (fn [{:keys [db]} _]
   {:dispatch-n
    (->> db
         :ants
         (map (fn [[coordinate {:keys [facing]}]] (movement/events db coordinate facing)))
         println-pass
         (mapv rand-nth))}))
