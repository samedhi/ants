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
  (let [{:keys [state steps] :as ant} (get ants coordinate)
        new-ant (as-> ant $
                  (select-keys $ [:facing])
                  (assoc $ :coordinate coordinate))]
    (case state
      :lost ants
      :reversed (update-in ants [coordinate :steps] pop)
      :foraging (update-in ants [coordinate :steps] (fnil conj []) new-ant))))

(defn drop-pheromone-type [db kind coordinate magnitude]
  (let [{:keys [tick]} db
        old-magnitude (-> db :pheromones kind (get coordinate) :magnitude (or 0))
        new-magnitude (+ old-magnitude magnitude)]
    (assoc-in db [:pheromones kind coordinate] {:tick tick :magnitude new-magnitude})))

(defn drop-pheromone [db coordinate]
  (let [{:keys [tick]} db
        {:keys [has-food? state]} (ant-at db coordinate)
        foraging? (= :foraging state)]
    (cond-> db
      has-food? (drop-pheromone-type :food coordinate 25)
      foraging? (drop-pheromone-type :path coordinate 5))))

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
      (assoc :state :foraging :steps [] :stuck-count 0)
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
       (harvested-handler coordinate)
       (drop-pheromone coordinate))))

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

(defn decay-coordinate [new-tick coordinate->pheromone coordinate pheromone-map]
  (let [{:keys [magnitude tick]} pheromone-map
        new-pheromones {:tick new-tick :magnitude (pow magnitude config/decay-rate)}]
    (if (<= 1.5 magnitude)
      (assoc  coordinate->pheromone coordinate new-pheromones)
      coordinate->pheromone)))

(re-frame/reg-event-db
 :decay
 (fn [db _]
   (let [{:keys [tick]} db
         decay-coordinate-with-tick (partial decay-coordinate tick)
         decay-all-coordinates #(reduce-kv decay-coordinate-with-tick {} %)]
     (update-in db [:pheromones :food] decay-all-coordinates))))

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
   (let [{:keys [tick] :as new-db} (update db :tick inc)]
     {:db new-db

      :dispatch-n
      (concat
       [[:decay]]
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
   (drop-pheromone-type db :food coordinate 25)))

(re-frame/reg-event-db
 :drop-path-pheromone
 (fn [db [_ coordinate]]
   (drop-pheromone-type db :path coordinate 5)))

(re-frame/reg-event-fx
 :consider-if-you-are-lost
 (fn [{:keys [db]} [_ coordinate]]
   (let [{:keys [stuck-count max-steps]} (ant-at db coordinate)
         i (-> max-steps (- stuck-count) rand-int)]
     (println :consider-if-you-are-lost coordinate stuck-count max-steps i)
     (when (<= i 0)
       (println :consider-if-you-are-lost-fired!)
       {:dispatch [:lost coordinate]}))))
