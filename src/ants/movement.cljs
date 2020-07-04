(ns ants.movement
  (:require
   [ants.config :as config]))

(defn blind-options [coordinate facing]
  (let [[x-old y-old] coordinate
        facing->coordinate-deltas (-> coordinate
                                      first
                                      even?
                                      {true :even false :odd}
                                      config/potential->coordinate-deltas)]
    (->> facing
         config/facing->potentials
         (map facing->coordinate-deltas)
         (map (fn [[x-delta y-delta]] [(+ x-old x-delta) (+ y-old y-delta)])))))
