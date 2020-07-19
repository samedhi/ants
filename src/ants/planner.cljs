(ns ants.planner
  (:require
   [ants.config :as config]
   [ants.subs :as subs]))

(defn coordinate-options [db old-coordinate old-facing]
  (let [{:keys [tile-magnitude row-count column-count pheromones]} db
        [x-old y-old] old-coordinate
        facing->coordinate-deltas (-> x-old
                                      even?
                                      config/even-row?->facing->coordinate-delta)]
    (for [new-facing (config/facing->potentials old-facing)
          :let [[x-delta y-delta] (facing->coordinate-deltas new-facing)
                new-coordinate [(mod (+ x-old x-delta) row-count)
                                (mod (+ y-old y-delta) column-count)]
                tile-pheromone-weight (subs/pheromone-sum pheromones new-coordinate)
                weight (+ tile-magnitude tile-pheromone-weight)]]
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

(defn move-option->event [{:keys [old-coordinate new-coordinate new-facing]}]
  ;; TODO: I don't really think you need a new-facing here, derive it in delta.
  [:move old-coordinate new-coordinate new-facing])

(defn move-actions [db coordinate facing]
  (let [{:keys [row-count column-count ants pheromones]} db]
    (some-> (coordinate-options db coordinate facing)
            seq
            select-coordinate-option
            move-option->event
            vector)))

(defn rotate-actions [coordinate facing]
  (let [[left _ right] (config/facing->potentials facing)]
    ;; TODO: Why don't we just pass in :left & :right as arguments, details in delta.
    (if (zero? (rand-int 2))
      [[:rotate coordinate left]]
      [[:rotate coordinate right]])))

(defn special-actions [db coordinate]
  (let [{:keys [ants food entrences]} db
        {:keys [max-steps steps has-food?
                stuck-count state facing state] :as ant} (get ants coordinate)
        lost? (= state :lost)
        over-colony? (contains? entrences coordinate)
        over-food? (contains? food coordinate)
        steps-count (count steps)]
    (cond
      (and over-colony? (not= state :foraging))
      [[:drop-food coordinate] [:reset coordinate]]

      (and (not lost?) (not has-food?) over-food?)
      [[:reverse coordinate] [:grab-food coordinate]]

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
    (or (special-actions db coordinate)
        (move-actions db coordinate facing)
        (rotate-actions coordinate facing))))
