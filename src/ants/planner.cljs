(ns ants.planner
  (:require
   [ants.config :as config]
   [ants.subs :as subs]))

(defn facing-coordinates
  "Return 3 facing-coordinates when at 'coordinate in 'facing direction."
  [db coordinate facing]
  (let [{:keys [row-count column-count]} db
        [x-old y-old] coordinate
        facing->coordinate-deltas (-> x-old
                                      even?
                                      config/even-row?->facing->coordinate-delta)]
    (for [new-facing (config/facing->potentials facing)
          :let [[x-delta y-delta] (facing->coordinate-deltas new-facing)
                new-coordinate [(mod (+ x-old x-delta) row-count)
                                (mod (+ y-old y-delta) column-count)]]]
      {:facing new-facing
       :coordinate new-coordinate})))

#_(facing-coordinates
 {:row-count 10
  :column-count 10}
 [2 2]
 :northeast)

(defn coordinate-options [db old-coordinate old-facing]
  (let [{:keys [tile-magnitude pheromones ants]} db
        {food-pheromones :food path-pheromones :path} pheromones
        {:keys [state]} (get ants old-coordinate)
        facing-coordinates (facing-coordinates db old-coordinate old-facing)]
    (for [{new-facing :facing new-coordinate :coordinate} facing-coordinates
          :let [food-magnitude (subs/pheromone-magnitude food-pheromones new-coordinate)
                path-magnitude (subs/pheromone-magnitude path-pheromones new-coordinate)
                weight (if (= state :foraging)
                         (+ tile-magnitude food-magnitude path-magnitude)
                         (+ tile-magnitude path-magnitude))]]
      {:old-coordinate old-coordinate
       :new-coordinate new-coordinate
       :old-facing old-facing
       :new-facing new-facing
       :weight weight})))

(defn select-coordinate-option [coordinate-options]
  (let [weights (map :weight coordinate-options)
        sum-weights (rest (reductions + 0 weights))
        rand-weight (-> sum-weights last rand-int)]
    (loop [[co & co-rest] coordinate-options
           [sw & sw-rest] sum-weights]
      (if (< rand-weight sw)
        co
        (recur co-rest sw-rest)))))

(defn remove-when-obstacle [db coordinate-options]
  (let [{:keys [ants]} db]
    (remove #(contains? ants (:new-coordinate %)) coordinate-options)))

(defn move-option->event [{:keys [old-coordinate new-coordinate new-facing]}]
  ;; TODO: I don't really think you need a new-facing here, derive it in delta.
  [:move old-coordinate new-coordinate new-facing])

(defn move-actions [db coordinate facing]
  (let [{:keys [row-count column-count ants pheromones]} db]
    (some->> (coordinate-options db coordinate facing)
             (remove-when-obstacle db)
             seq ;; NOTE: Used for short circuit
             select-coordinate-option
             move-option->event
            vector)))

(defn rotate-actions [coordinate facing]
  (let [[left _ right] (config/facing->potentials facing)]
    ;; TODO: Why don't we just pass in :left & :right as arguments, details in delta.
    (if (zero? (rand-int 2))
      [[:rotate coordinate left]]
      [[:rotate coordinate right]])))

(defn first-coordinate-contains?
  "First element in 'ms with :coordinate in 'associative"
  [associative ms]
  (first (filter #(contains? associative (:coordinate %)) ms)))

(defn special-actions [db coordinate]
  (let [{:keys [ants food entrences]} db
        {:keys [max-steps steps has-food?
                stuck-count state facing state] :as ant} (get ants coordinate)
        foraging? (= state :foraging)
        lost? (= state :lost)
        reversed? (= state :reversed)
        over-colony? (contains? entrences coordinate)
        over-food? (contains? food coordinate)
        steps-count (count steps)
        facing-coordinates (facing-coordinates db coordinate facing)
        visible-colony (first-coordinate-contains? entrences facing-coordinates)
        visible-food (first-coordinate-contains? food facing-coordinates)]
    (cond
      (and foraging? visible-food)
      [[:move coordinate (:coordinate visible-food) (:facing visible-food)]]

      (and reversed? visible-colony)
      [[:move coordinate (:coordinate visible-colony) (:facing visible-colony)]]

      (and over-colony? (not= state :foraging))
      [[:drop-food coordinate] [:reset coordinate]]

      (and (not has-food?) over-food?)
      [[:reverse coordinate] [:grab-food coordinate]]

      (and (not lost?) (not reversed?) (<= max-steps steps-count))
      [[:reverse coordinate]])))

(defn events [db coordinate ant]
  (let [{:keys [facing]} ant]
    (or (special-actions db coordinate)
        (move-actions db coordinate facing)
        (rotate-actions coordinate facing))))
