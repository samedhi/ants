(ns ants.planner
  (:require
   [ants.config :as config]
   [ants.subs :as subs]))

(defn blind-options [coordinate old-facing]
  (let [[x-old y-old] coordinate
        facing->coordinate-deltas (-> coordinate
                                      first
                                      even?
                                      {true :even false :odd}
                                      config/even-row->facing->coordinate-delta)
        high-priority-facing (set (config/facing->potentials old-facing))]
    (for [facing config/facings
          :let [[x-delta y-delta] (facing->coordinate-deltas facing)]]
      {:x (+ x-old x-delta)
       :y (+ y-old y-delta)
       :facing facing
       :coordinate coordinate
       :exponent (if (contains? high-priority-facing facing) 2 1)})))

(defn remove-off-plane-coordinates [coordinates row-count column-count]
  (->> coordinates
       (filter #(< -1 (:x %) row-count))
       (filter #(< -1 (:y %) column-count))))

(defn remove-collisions [coordinates ants]
  (remove #(contains? ants [(:x %) (:y %)]) coordinates))

(defn pow [n m]
  (js/Math.pow n m))

(defn calculate-tile-weight [pheromones coordinate]
  (let [{:keys [x y exponent]} coordinate
        tile-pheromone-weight (subs/pheromone-sum pheromones [x y])
        weight (+ config/tile-base-weight tile-pheromone-weight)
        exponented-weight (pow weight exponent)]
    (assoc coordinate :weight exponented-weight)))

(defn select-coordinate [coordinates pheromones]
  (let [weighted-coordinates (map (partial calculate-tile-weight pheromones) coordinates)
        sums (map :weight weighted-coordinates)
        lookups (rest (reductions + 0 sums))
        sum-weighted-coordinates (map #(assoc %1 :sum-weight %2) weighted-coordinates lookups)
        lookups-sum (apply + sums)
        n (rand-int lookups-sum)
        selected (first (drop-while #(<= (:sum-weight %) n) sum-weighted-coordinates))]
    selected))

(defn move-option->event [{:keys [x y facing coordinate] :as move-option}]
  [:move coordinate [x y] facing])

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

(defn println-pass [o]
  (println o)
  o)

(defn pre-special-actions [db coordinate]
  (let [{:keys [ants food entrences]} db
        {:keys [max-steps steps has-food?
                stuck-count state facing state] :as ant} (get ants coordinate)
        lost? (= state :lost)
        over-colony? (contains? entrences coordinate)
        over-food? (contains? food coordinate)
        steps-count (count steps)]
    (cond
      (and over-colony? (not= state :foraging))
      [[:drop-food] [:reset coordinate]]

      (and (not lost?) (not has-food?) over-food?)
      [[:grab-food coordinate] [:reverse coordinate] [:drop-pheromone coordinate]]

      (and (not lost?) (not over-colony?) (zero? steps-count))
      [[:drop-food coordinate] [:lost coordinate]]

      (and (not lost?)
           (<= 2 stuck-count)
           (<= (rand-int max-steps) stuck-count))
      [[:drop-food coordinate] [:lost coordinate]]

      (= state :reversed)
      [[:reverse-move coordinate]]

      (and (not lost?) (<= max-steps steps-count))
      [[:reverse coordinate]])))

(defn events [db coordinate ant]
  (let [{:keys [facing]} ant]
    (or (pre-special-actions db coordinate)
        (move-options db coordinate facing)
        (rotate-options coordinate facing))))
