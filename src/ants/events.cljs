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

(defn modify-steps [ants coordinate]
  (let [{:keys [reversed?] :as ant} (get ants coordinate)
        new-ant (as-> ant $
                  (select-keys $ [:facing])
                  (assoc $ :coordinate coordinate))]
    (if reversed?
      (update-in ants [coordinate :steps] pop)
      (update-in ants [coordinate :steps] (fnil conj []) new-ant))))

(defn move-ant [ants old-coordinate new-coordinate new-facing]
  (-> ants
      (modify-steps old-coordinate)
      (rename-key old-coordinate new-coordinate)
      (update new-coordinate assoc :facing new-facing)))

(defn move [db [_ old-coordinate new-coordinate new-facing]]
  (if (-> db :ants (contains? new-coordinate))
    db
    (update db :ants move-ant old-coordinate new-coordinate new-facing)))

(re-frame/reg-event-db
 :move
 move)

(re-frame/reg-event-db
 :drop-pheromone
 (fn [db [_ coordinate]]
   (let [{:keys [tick]} db]
     (assoc-in db [:pheromones coordinate tick] 10))))

(re-frame/reg-event-db
 :reverse-move
 (fn [db [_ old-coordinate]]
   (let [{:keys [coordinate facing]} (-> db :ants (get old-coordinate) :steps peek)
         reverse-facing (config/facing->reverse-facing facing)]
     (move db [:move old-coordinate coordinate reverse-facing]))))

(re-frame/reg-event-db
 :rotate
 (fn [db [_ coordinate new-facing]]
   (update-in db [:ants coordinate] assoc :facing new-facing)))

(re-frame/reg-event-db
 :reverse
 (fn [db [_ coordinate]]
   (-> db
       (update-in [:ants coordinate :reversed?] not)
       (update-in [:ants coordinate :facing] config/facing->reverse-facing))))

(re-frame/reg-event-db
 :reset
 (fn [db [_ coordinate]]
   (-> db
       (update-in [:ants coordinate] dissoc :steps :reversed?)
       (update-in [:ants coordinate :facing] config/facing->reverse-facing))))

(defn harvested-handler [db coordinate]
  (if (-> db :food (get coordinate) zero?)
    (update db :food dissoc coordinate)
    db))

(re-frame/reg-event-db
 :grab-food
 (fn [db [_ coordinate]]
   (-> db
       (assoc-in [:ants coordinate :has-food?] true)
       (update-in [:food coordinate] dec)
       (harvested-handler coordinate))))

(re-frame/reg-event-db
 :drop-food
 (fn [db [_ coordinate]]
   (-> db
       (update-in [:ants coordinate] dissoc :has-food?)
       (update :colony-food (fnil inc 0)))))

(defn decay-pheromone [tick pheromone]
  (reduce-kv
   (fn [m k v]
     (println :decay-pheromone k v)
     (if (<= v 0.25)
       (dissoc m k)
       (update m k / 2)))
     pheromone
     pheromone))

(defn decay-coordinate [tick m coordinate pheromone]
  (assoc m coordinate (decay-pheromone tick pheromone)))

(re-frame/reg-event-db
 :decay
 (fn [db _]
   (let [{:keys [tick]} db]
     (update db :pheromones #(reduce-kv (partial decay-coordinate tick) {} %)))))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-fx
 :tick
 (fn [{:keys [db]} _]
   (let [{:keys [tick]} db
         new-db (assoc db :tick (inc tick))]
     {:db
      new-db

      :dispatch-n
      (->> db
           :ants
           (map (fn [[coordinate ant]]
                  (movement/events new-db coordinate ant)))
           ;; (cons (when (zero? (mod tick 10)) [[:decay]]))
           (apply concat))})))
