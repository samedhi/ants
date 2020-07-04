(ns ants.movement
  (:require
   [ants.config :as config]))

(defn blind-options [coordinate facing]
  (let [[x-old y-old] coordinate
        facing->coordinate-deltas (-> coordinate
                                      first
                                      even?
                                      {true :even false :odd}
                                      config/even-row->facing->coordinate-delta)]
    (->> facing
         config/facing->potentials
         (map facing->coordinate-deltas)
         (map (fn [[x-delta y-delta]] [(+ x-old x-delta) (+ y-old y-delta)])))))

(defn remove-off-plane-coordinates [coordinates row-count column-count]
  (->> coordinates
       (filter #(< -1 (first %) row-count))
       (filter #(< -1 (second %) column-count))))

(defn move-options [db coordinate facing]
  (let [{:keys [row-count column-count]} db]
    (-> coordinate
        (blind-options facing)
        (remove-off-plane-coordinates row-count column-count))))

(defn rotate-options [facing]
  (let [[left _ right] (config/facing->potentials facing)]
    [left right]))

(defn events [db coordinate facing]
  (or (map #(vector [:move coordinate %]) (move-options db coordinate facing))
      (map #(vector [:rotate coordinate %]) (rotate-options facing))))
