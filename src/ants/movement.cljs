(ns ants.movement
  (:require
   [ants.config :as config]))

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

(defn permissive-rand-nth [xs]
  (when (-> xs count pos?)
    (rand-nth xs)))

(defn move-options [db coordinate facing]
  (let [{:keys [row-count column-count ants]} db]
    (some-> coordinate
            (blind-options facing)
            (remove-off-plane-coordinates row-count column-count)
            (remove-collisions ants)
            permissive-rand-nth
            move-option->event
            vector)))

(defn rotate-options [coordinate facing]
  (let [[left _ right] (config/facing->potentials facing)]
    (if (zero? (rand-int 2))
      [[:rotate coordinate left]]
      [[:rotate coordinate right]])))

(defn special-actions [db coordinate]
  (let [{:keys [max-steps steps reversed? has-food?] :as ant} (-> db :ants (get coordinate))
        {:keys [food entrences]} db
        over-colony? (contains? entrences coordinate)
        over-food? (contains? food coordinate)
        steps-count (count steps)]
    (cond
      (and reversed? (pos? steps-count))
      [[:reverse-move coordinate]]

      (and over-food? (not has-food?))
      [[:grab-food coordinate] [:reverse coordinate]]

      (and (not reversed?) (<= max-steps (count steps)))
      [[:reverse coordinate]]

      (and over-colony? has-food?)
      [[:drop-food coordinate] [:reset coordinate]]

      (and over-colony? reversed?)
      [[:reset coordinate]]

      (and over-colony? (pos? steps-count))
      [[:reset coordinate]])))

(defn events [db coordinate ant]
  (let [{:keys [facing]} ant]
    (or (special-actions db coordinate)
        (move-options db coordinate facing)
        (rotate-options coordinate facing))))
