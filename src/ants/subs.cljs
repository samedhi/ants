(ns ants.subs
  (:require
   [re-frame.core :as re-frame]
   [ants.util :as util]))

(enable-console-print!)

(re-frame/reg-sub
 :tick
 :tick)

(re-frame/reg-sub
 :greeting
 (fn [db]
   (:greeting db)))

(re-frame/reg-sub
 :pretty-print-db
 (fn [db]
   (util/pprint (dissoc db :pheromones))))

(re-frame/reg-sub
 :row-count
 :row-count)

(re-frame/reg-sub
 :selected-tool
 :selected-tool)

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

(def current-pheromone-max (atom 8))

(re-frame/reg-sub
 :pheromones-max
 :<- [:tick]
 :<- [:pheromones]
 (fn [[tick pheromones] _]
   (if (zero? (mod tick 10))
     (some->> (vals pheromones)
              (map vals)
              (map #(apply + %))
              (apply max)
              (reset! current-pheromone-max))
     @current-pheromone-max)))

(defn binary-divisions [n-max]
  (js/Math.pow
   2
   (js/Math.ceil
    (js/Math.log2
     (max 8 n-max)))))

(re-frame/reg-sub
 :pheromone-divisions
 :<- [:pheromones-max]
 (fn [pheromones-max _]
   (binary-divisions pheromones-max)))

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
