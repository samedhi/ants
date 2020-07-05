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
 :ant-at-tile
 :-> [:ants]
 (fn [ants [_ coordinate]]
   (get ants coordinate)))

(re-frame/reg-sub
 :tile-state
 (fn [[_ coordinate] _]
   (re-frame/subscribe [:ant-at-tile coordinate]))
 (fn [tile-state _]
   tile-state))
