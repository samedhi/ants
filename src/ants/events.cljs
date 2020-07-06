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

(defn conj-step [ants old-coordinate]
  (as-> ants $
    (get $ old-coordinate)
    (select-keys $ [:facing])
    (assoc $ :coordinate old-coordinate)
    (update-in ants [old-coordinate :steps] (fnil conj []) $)))

(defn move-ant [ants old-coordinate new-coordinate new-facing]
  (-> ants
      (conj-step old-coordinate)
      (rename-key old-coordinate new-coordinate)
      (update new-coordinate assoc :facing new-facing)))

(re-frame/reg-event-db
 :move
 (fn [db [_ old-coordinate new-coordinate new-facing]]
   (if (-> db :ants (contains? new-coordinate))
     db
     (update db :ants move-ant old-coordinate new-coordinate new-facing))))

(re-frame/reg-event-db
 :rotate
 (fn [db [_ coordinate new-facing]]
   (update-in db [:ants coordinate] assoc :facing new-facing)))

(re-frame/reg-event-db
 :reverse
 (fn [db [_ coordinate]]
   (update-in db [:ants coordinate :reversed?] not)))

(re-frame/reg-event-db
 :reset
 (fn [db [_ coordinate]]
   (update-in db [:ants coordinate] dissoc :steps :reversed?)))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-fx
 :tick
 (fn [{:keys [db]} _]
   {:dispatch-n
    (->> db
         :ants
         (map (fn [[coordinate {:keys [facing]}]] (movement/events db coordinate facing)))
         (mapv rand-nth))}))
