(ns ants.events
  (:require
   [ants.config :as config]
   [ants.movement :as movement]
   [clojure.set :as set]
   [re-frame.core :as re-frame]))

(defn rename-key [m k-old k-new]
  (set/rename-keys m {k-old k-new}))

(defn modify-steps [ants coordinate]
  (let [{:keys [reversed? steps] :as ant} (get ants coordinate)
        new-ant (as-> ant $
                  (select-keys $ [:facing])
                  (assoc $ :coordinate coordinate))]
    (if reversed?
      (update-in ants [coordinate :steps] pop)
      (update-in ants [coordinate :steps] (fnil conj []) new-ant))))

(defn move-ant [ants old-coordinate new-coordinate new-facing]
  (-> ants
      (update old-coordinate dissoc :stuck-count)
      (modify-steps old-coordinate)
      (rename-key old-coordinate new-coordinate)
      (update new-coordinate assoc :facing new-facing)))

(defn move [db old-coordinate new-coordinate new-facing]
  (if (-> db :ants (contains? new-coordinate))
    (update-in db [:ants old-coordinate :stuck-count] (fnil inc 0))
    (update db :ants move-ant old-coordinate new-coordinate new-facing)))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-fx
 :lost
 (fn [{:keys [db]} [_ coordinate]]
   {:db (-> db
            (assoc-in [:ants coordinate :state] :lost)
            (assoc-in [:ants coordinate :lost?] true)
            (update-in [:ants coordinate] dissoc :steps :reversed? :stuck-count))
    :dispatch-n [(when (-> db :ants (get coordinate) :has-food?) [:drop-food coordinate])]}))

(defn tile-at [db root-key coordinate]
  (get-in db [root-key coordinate]))

(defn ant-at [db coordinate]
  (tile-at db :ants coordinate))

(defn food-at [db coordinate]
  (tile-at db :food coordinate))

(re-frame/reg-event-db
 :drop-pheromone
 (fn [db [_ coordinate]]
   (let [{:keys [tick]} db
         current (-> db :pheromones (get coordinate) first second)
         max-delta (- config/max-pheromone current)]
     (assoc-in db [:pheromones coordinate] {tick (+ current (/ max-delta 2))}))))

(re-frame/reg-event-fx
 :move
 (fn [{:keys [db]} [_ old-coordinate new-coordinate new-facing]]
   (let [new-db (move db old-coordinate new-coordinate new-facing)
         tile-has-food? (pos? (food-at new-db new-coordinate))
         {:keys [state]} (ant-at db old-coordinate)
         moved? (nil? (ant-at new-db old-coordinate))
         foraging? (= :foraging state)]
     (merge
      {:db new-db}
      (when (and tile-has-food? moved? foraging?)
        {:dispatch-n [[:grab-food new-coordinate]
                      [:reverse new-coordinate]]})))))

(re-frame/reg-event-fx
 :reverse-move
 (fn [{:keys [db]} [_ old-coordinate]]
   (let [{:keys [has-food? steps] :as ant} (-> db :ants (get old-coordinate))
         {:keys [coordinate facing]} (peek steps)
         reverse-facing (config/facing->reverse-facing facing)
         new-db (move db old-coordinate coordinate reverse-facing)
         moved? (nil? (ant-at new-db old-coordinate))]
     (merge
      {:db new-db
       :dispatch-n [(when (and moved? has-food?) [:drop-pheromone coordinate])]}))))

(re-frame/reg-event-db
 :rotate
 (fn [db [_ coordinate new-facing]]
   (update-in db [:ants coordinate] assoc :facing new-facing)))

(re-frame/reg-event-db
 :reverse
 (fn [db [_ coordinate]]
   (-> db
       (assoc-in  [:ants coordinate :state] :returning)
       (update-in [:ants coordinate :reversed?] not)
       (update-in [:ants coordinate :facing] config/facing->reverse-facing))))

(re-frame/reg-event-db
 :reset
 (fn [db [_ coordinate]]
   (-> db
       (update-in [:ants coordinate] select-keys [:facing :max-steps :name])
       (assoc-in  [:ants coordinate :state] :foraging)
       (update-in [:ants coordinate :facing] config/facing->reverse-facing))))

(defn harvested-handler [db coordinate]
  (if (-> db :food (get coordinate) zero?)
    (update db :food dissoc coordinate)
    db))

(re-frame/reg-event-fx
 :grab-food
 (fn [{:keys [db]} [_ coordinate]]
   {:db (-> db
            (assoc-in [:ants coordinate :has-food?] true)
            (update-in [:food coordinate] dec)
            (harvested-handler coordinate))
    :dispatch [:drop-pheromone coordinate]}))

(re-frame/reg-event-db
 :drop-food
 (fn [db [_ coordinate]]
   (let [entrence? (-> db :entrences (contains? coordinate))]
     (cond-> db
       entrence?       (update-in [:colony-food] (fnil inc 0))
       (not entrence?) (update-in [:food coordinate] (fnil inc 0))
       true            (update-in [:ants coordinate] dissoc :has-food?)))))

(defn decay-pheromone [tick pheromone]
  (reduce-kv
   (fn [m k v]
     (if (<= v 0.25)
       (dissoc m k)
       (update m k * 0.9)))
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
           (cons (when (zero? (mod tick 10)) [[:decay]]))
           (apply concat))})))

(re-frame/reg-event-db
 :select-tool
 (fn [db [_ tool]]
   (assoc db :selected-tool tool)))

(re-frame/reg-event-fx
 :tile-clicked
 (fn [{:keys [db]} [_ coordinate]]
   (let [{:keys [selected-tool]} db]
     {:dispatch [selected-tool coordinate]})))

(re-frame/reg-event-db
 :drop-ant
 (fn [db [_ coordinate]]
   (let [ant-name (->> db :ants count inc (str "ant-"))
         new-ant (assoc config/default-ant :name ant-name)]
     (assoc-in db [:ants coordinate] new-ant))))

(re-frame/reg-event-db
 :drop-colony
 (fn [db [_ coordinate]]
   (if (contains? (:entrences db) coordinate)
     (update-in db [:entrences] disj coordinate)
     (update-in db [:entrences] conj coordinate))))

(re-frame/reg-event-db
 :drop-10-food
 (fn [db [_ coordinate]]
   (update-in db [:food coordinate] (fnil + 0) 10)))

(re-frame/reg-event-db
 :drop-100-food
 (fn [db [_ coordinate]]
   (update-in db [:food coordinate] (fnil + 0) 100)))

(re-frame/reg-event-fx
 :consider-if-you-are-lost
 (fn [db [_ coordinate]]
   (let [{:keys [stuck-count max-steps]} (get (:ants db) coordinate)]
     (if (-> max-steps (- stuck-count) rand-int pos? not)
       {:dispatch [:lost coordinate]}))))
