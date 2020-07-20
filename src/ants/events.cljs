(ns ants.events
  (:require
   [ants.config :as config]
   [ants.planner :as planner]
   [cljs.core.async :as async]
   [clojure.set :as set]
   [re-frame.core :as re-frame]))

(defn tile-at [db root-key coordinate]
  (get-in db [root-key coordinate]))

(defn ant-at [db coordinate]
  (tile-at db :ants coordinate))

(defn food-at [db coordinate]
  (tile-at db :food coordinate))

(defn colony-at? [db coordinate]
  (-> db :entrences (contains? coordinate)))

(defn modify-steps [ants coordinate]
  (let [{:keys [state]} (get ants coordinate)]
    (if (= state :foraging)
      (update-in ants [coordinate :steps] (fnil conj #{}) coordinate)
      ants)))

(defn drop-pheromone-type [db kind coordinate]
  (let [{:keys [tick]} db
        magnitude (-> db :pheromones-meta kind :magnitude)
        old-magnitude (-> db :pheromones kind (get coordinate) :magnitude (or 0))
        new-magnitude (+ old-magnitude magnitude)]
    (assoc-in db [:pheromones kind coordinate] {:tick tick :magnitude new-magnitude})))

(defn drop-food-pheromone [db coordinate]
  (let [{:keys [pheromones]} db
        {food-pheromones :food path-pheromones :path} pheromones
        {food-magnitude :magnitude} (get food-pheromones coordinate)
        {path-magnitude :magnitude} (get path-pheromones coordinate)]
    (if (< food-magnitude (* 10 path-magnitude))
      (drop-pheromone-type db :food coordinate)
      db)))

(defn drop-pheromone [db coordinate]
  (let [{:keys [tick]} db
        {:keys [has-food? state steps]} (ant-at db coordinate)
        {:keys [magnitude]} (-> db :pheromones :path (get coordinate))
        foraging? (= :foraging state)
        cycle? (contains? steps coordinate)]
    (cond-> db
      has-food?
      (drop-food-pheromone coordinate)

      (and foraging? (not cycle?))
      (drop-pheromone-type :path coordinate))))

(defn move-ant [ants old-coordinate new-coordinate new-facing]
  (-> ants
      (assoc-in [old-coordinate :stuck-count] 0)
      (modify-steps old-coordinate)
      (update old-coordinate assoc :facing new-facing)
      (set/rename-keys {old-coordinate new-coordinate})))

(defn move [db old-coordinate new-coordinate new-facing]
  (if (-> db :ants (contains? new-coordinate))
    (update-in db [:ants old-coordinate :stuck-count] (fnil inc 0))
    (-> db
        (update :ants move-ant old-coordinate new-coordinate new-facing)
        (drop-pheromone new-coordinate))))

(defn reset-ant [ant]
  (-> ant
      (assoc :state :foraging :steps #{} :stuck-count 0)
      (update :facing config/facing->reverse-facing)))

(defn harvested-handler [db coordinate]
  (if (-> db :food (get coordinate) zero?)
    (update db :food dissoc coordinate)
    db))

(defn not-pos? [n]
  (-> n pos? not))

(defn dec-and-dissoc-at-zero [m k]
  (let [v-old (get m k)
        v-new (dec v-old)]
    (if (not-pos? v-new)
      (dissoc m k)
      (assoc m k v-new))))

(defn pow [n m]
  (js/Math.pow n m))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-db
 :lost
 (fn [db [_ coordinate]]
   (-> db
       (assoc-in [:ants coordinate :state] :lost)
       (assoc-in [:ants coordinate :steps] []))))

(re-frame/reg-event-db
 :move
 (fn [db [_ old-coordinate new-coordinate new-facing]]
   (move db old-coordinate new-coordinate new-facing)))

(re-frame/reg-event-db
 :reverse-move
 (fn [db [_ old-coordinate]]
   (let [{:keys [coordinate facing]} (-> db :ants (get old-coordinate) :steps peek)
         reverse-facing (config/facing->reverse-facing facing)]
     (move db old-coordinate coordinate reverse-facing))))

(re-frame/reg-event-db
 :rotate
 (fn [db [_ coordinate new-facing]]
   (update-in db [:ants coordinate] assoc :facing new-facing)))

(re-frame/reg-event-db
 :reverse
 (fn [db [_ coordinate]]
   (-> db
       (assoc-in  [:ants coordinate :state] :reversed)
       (update-in [:ants coordinate :facing] config/facing->reverse-facing))))

(re-frame/reg-event-db
 :reset
 (fn [db [_ coordinate]]
   (update-in db [:ants coordinate] reset-ant)))

(re-frame/reg-event-db
 :grab-food
 (fn [db [_ coordinate]]
   (-> db
       (assoc-in [:ants coordinate :has-food?] true)
       (update :food dec-and-dissoc-at-zero coordinate)
       (harvested-handler coordinate))))

(re-frame/reg-event-db
 :drop-food
 (fn [db [_ coordinate]]
   (let [colony? (colony-at? db coordinate)
         {:keys [has-food?]} (ant-at db coordinate)]
     (if has-food?
       (cond-> db
         colony?       (update-in [:colony-food] (fnil inc 0))
         (not colony?) (update-in [:food coordinate] (fnil inc 0))
         true          (assoc-in  [:ants coordinate :has-food?] false))
       db))))

(defn decay-coordinate [db kind coordinate]
  (let [{new-tick :tick} db
        {:keys [decay-rate]} (get-in db [:pheromones-meta kind])
        {old-tick :tick old-magnitude :magnitude} (get-in db [:pheromones kind coordinate])
        new-magnitude (->> (- new-tick old-tick)
                           (pow decay-rate)
                           (* old-magnitude))]
    (if (<= 1.5 new-magnitude)
      (assoc-in db [:pheromones kind coordinate] {:tick new-tick :magnitude new-magnitude})
      (update-in db [:pheromones kind] dissoc coordinate))))

(re-frame/reg-event-db
 :decay-all
 (fn [db _]
   (reduce
    (fn [db [kind coordinate]]
      (decay-coordinate db kind coordinate))
    db
    (for [[kind m] (-> db :pheromones)
          [coordinate _] m]
      [kind coordinate]))))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-db
 :close-work-chan
 (fn [db [_ work-complete-chan]]
   (async/close! work-complete-chan)
   db))

(re-frame/reg-event-fx
 :tick
 (fn [{:keys [db]} [_ work-complete-chan]]
   (let [{:keys [tick entrences food] :as new-db} (update db :tick inc)]
     {:db new-db

      :dispatch-n
      (concat
       [[:decay-all]]
       (apply
        concat
        (map (fn [[coordinate ant]]
               (planner/events new-db coordinate ant)) (:ants db)))
       [[:close-work-chan work-complete-chan]])})))

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

(re-frame/reg-event-db
 :drop-food-pheromone
 (fn [db [_ coordinate]]
   (drop-pheromone-type db :food coordinate)))

(re-frame/reg-event-db
 :drop-path-pheromone
 (fn [db [_ coordinate]]
   (drop-pheromone-type db :path coordinate)))

(re-frame/reg-event-db
 :merge-db
 (fn [db [_ merge-db]]
   (merge db merge-db)))
