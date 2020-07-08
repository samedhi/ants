(ns ants.subs
  (:require
   [re-frame.core :as re-frame]
   [ants.util :as util]))

(re-frame/reg-sub
 :greeting
 (fn [db]
   (:greeting db)))

(re-frame/reg-sub
 :pretty-print-db
 (fn [db]
   (util/pprint db)))

(re-frame/reg-sub
 :row-count
 :row-count)

(re-frame/reg-sub
 :column-count
 :column-count)

(re-frame/reg-sub
 :ants
 :ants)

(re-frame/reg-sub
 :time-between-ticks
 :time-between-ticks)

(re-frame/reg-sub
 :entrences
 :entrences)

(re-frame/reg-sub
 :food
 :food)

(re-frame/reg-sub
 :pheromones
 :pheromones)

(re-frame/reg-sub
 :pheromones-max
 :<- [:pheromones]
 (fn [pheromones _]
   (->> (map vals pheromones)
        (mapcat +)
        seq
        (or [0])
        (apply max))))

(defn scaled-opacity [n-max]
  (js/Math.pow
   2
   (js/Math.ceil
    (js/Math.log2
     (max 8 n-max)))))

(re-frame/reg-sub
 :pheromone-divisions
 :<- [:pheromones-max]
 (fn [pheromones-max _]
   (scaled-opacity pheromones-max)))

(defn pheromone-sum [pheromones coordinate]
  (if-let [m (get pheromones coordinate)]
    (apply + (vals m))
    0))

(re-frame/reg-sub
 :pheromone-map
 :<- [:pheromones]
 :<- [:pheromone-divisions]
 (fn [[pheromones pheromone-divisions] [_ coordinate]]
   (let [pheromone (pheromone-sum pheromones coordinate)]
     {:pheromone (pheromone-sum pheromones coordinate)
      :pheromone-opacity (/ pheromone pheromone-divisions)})))

(re-frame/reg-sub
 :ant-at-tile
 :<- [:ants]
 (fn [ants [_ coordinate]]
   (get ants coordinate)))

(re-frame/reg-sub
 :tile-state
 (fn [[_ coordinate] _]
   [(re-frame/subscribe [:ant-at-tile coordinate])
    (re-frame/subscribe [:entrences])
    (re-frame/subscribe [:food])
    (re-frame/subscribe [:pheromone-map coordinate])])
 (fn [[ant entrences food pheromone-map] [_ coordinate]]
   (merge {:food (food coordinate 0)}
          pheromone-map
          ant
          (when (contains? entrences coordinate)
            {:entrence? true}))))
