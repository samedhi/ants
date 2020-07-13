(ns ants.movement
  (:require
   [ants.config :as config]
   [ants.subs :as subs]))

(defn blind-options [coordinate old-facing]
  (let [[x-old y-old] coordinate
        facing->coordinate-deltas (-> coordinate
                                      first
                                      even?
                                      {true :even false :odd}
                                      config/even-row->facing->coordinate-delta)]
    (for [facing (config/facing->potentials old-facing)
          :let [[x-delta y-delta] (facing->coordinate-deltas facing)]]
      {:x (+ x-old x-delta)
       :y (+ y-old y-delta)
       :facing facing
       :coordinate coordinate})))

(defn remove-off-plane-coordinates [coordinates row-count column-count]
  (->> coordinates
       (filter #(< -1 (:x %) row-count))
       (filter #(< -1 (:y %) column-count))))

(defn remove-collisions [coordinates ants]
  (remove #(contains? ants [(:x %) (:y %)]) coordinates))

(defn move-option->event [{:keys [x y facing coordinate] :as move-option}]
  [:move coordinate [x y] facing])

(defn select-coordinate [coordinates pheromones]
  (let [pheromone-sums (map #(subs/pheromone-sum pheromones [(:x %) (:y %)]) coordinates)
        sums (map inc pheromone-sums)
        lookups (rest (reductions + 0 sums))
        n (rand-int (apply + sums))
        coordinates-and-lookups (map vector coordinates lookups)
        drop-coordinates-and-lookups (drop-while #(<= (second %) n) coordinates-and-lookups)]
    (ffirst drop-coordinates-and-lookups)))

(defn move-options [db coordinate facing]
  (let [{:keys [row-count column-count ants pheromones]} db]
    (some-> coordinate
            (blind-options facing)
            (remove-off-plane-coordinates row-count column-count)
            (remove-collisions ants)
            (select-coordinate pheromones)
            move-option->event
            vector)))

(defn rotate-options [coordinate facing]
  (let [[left _ right] (config/facing->potentials facing)]
    (if (zero? (rand-int 2))
      [[:rotate coordinate left]]
      [[:rotate coordinate right]])))

(defn special-actions [db coordinate]
  (let [{:keys [ants food entrences]} db
        {:keys [max-steps steps reversed? has-food? stuck-count state] :as ant} (get ants coordinate)
        over-colony? (contains? entrences coordinate)
        over-food? (contains? food coordinate)
        steps-count (count steps)]
    (cond
      (and over-colony? (not= state :foraging))
      [[:reset coordinate]]

      (< steps-count stuck-count)
      [[:admit-your-lost coordinate]]

      reversed?
      [[:reverse-move coordinate]]

      (<= max-steps (count steps))
      [[:reverse coordinate]])))

(defn events [db coordinate ant]
  (let [{:keys [facing]} ant]
    (or (special-actions db coordinate)
        (move-options db coordinate facing)
        (rotate-options coordinate facing))))
