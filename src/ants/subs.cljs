(ns ants.subs
  (:require
   [re-frame.core :as re-frame]
   [ants.util :as util]
   [ants.config :as config]))

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
   (util/pprint
    (into (sorted-map)
          (-> db
              ;; (assoc :pheromones "...")
              (assoc :ants "..."))))))

(re-frame/reg-sub
 :pretty-print-ants-seq
 :<- [:ants]
 (fn [ants]
   (for [[coordinate ant] (sort-by #(-> % second :name) ants)]
     (as-> ant $
         (assoc $ :coordinate coordinate)
         (update $ :steps count)
         (into (sorted-map) $)
         (util/pprint $)))))

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

(re-frame/reg-sub
 :pheromones->food
 :<- [:pheromones]
 (fn [pheromones]
   (:food pheromones)))

(re-frame/reg-sub
 :pheromones->forage
 :<- [:pheromones]
 (fn [pheromones]
   (:forage pheromones)))

(re-frame/reg-sub
 :pheromones-max
 :<- [:tick]
 :<- [:pheromones->food]
 :<- [:pheromones->forage]
 (fn [[tick pheromones->food pheromones->forage]]
   (->> (concat (vals pheromones->food) (vals pheromones->forage))
        (map :magnitude)
        (apply max 0))))

(defn binary-divisions [n-max]
  (js/Math.pow
   2
   (js/Math.ceil
    (js/Math.log2
     (max config/min-magnitude n-max)))))

(re-frame/reg-sub
 :pheromone-divisions
 :<- [:pheromones-max]
 (fn [pheromones-max _]
   (binary-divisions pheromones-max)))

(defn pheromone-magnitude [pheromones coordinate]
  (-> pheromones
      (get coordinate)
      (:magnitude 0)))

(re-frame/reg-sub
 :pheromones->food->coordinate
 :<- [:pheromones->food]
 (fn [pheromones->food [_ coordinate]]
   (pheromone-magnitude pheromones->food coordinate)))

(re-frame/reg-sub
 :pheromones->forage->coordinate
 :<- [:pheromones->forage]
 (fn [pheromones->forage [_ coordinate]]
   (pheromone-magnitude pheromones->forage coordinate)))

(defn pheromone-sum [pheromones->food pheromones->forage coordinate]
  (+ (pheromone-magnitude pheromones->food coordinate)
     (pheromone-magnitude pheromones->forage coordinate)))

(re-frame/reg-sub
 :pheromones-total-magnitude
 (fn [[_ coordinate] _]
   [(re-frame/subscribe [:pheromones->food->coordinate coordinate])
    (re-frame/subscribe [:pheromones->forage->coordinate coordinate])])
 (fn [[food-pheromones forage-pheromones] _]
   (+ food-pheromones forage-pheromones)))

(re-frame/reg-sub
 :pheromone-map
 (fn [[_ coordinate] _]
   [(re-frame/subscribe [:pheromones-total-magnitude coordinate])
    (re-frame/subscribe [:pheromone-divisions])])
 (fn [[total-magnitude divisions] _]
   {:pheromone total-magnitude
    :pheromone-opacity (/ total-magnitude divisions)}))

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
