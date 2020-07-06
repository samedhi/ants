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

(defn move-options [db coordinate facing]
  (let [{:keys [row-count column-count ants]} db]
    (-> coordinate
        (blind-options facing)
        (remove-off-plane-coordinates row-count column-count)
        (remove-collisions ants))))

(defn move-option->event [{:keys [x y facing coordinate]}]
  [:move coordinate [x y] facing])

(defn rotate-options [facing]
  (let [[left _ right] (config/facing->potentials facing)]
    [left right]))

(defn events [db coordinate facing]
  (or (seq (map move-option->event (move-options db coordinate facing)))
      (map #(vector :rotate coordinate %) (rotate-options facing))))
