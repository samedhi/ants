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
 :ant-at-tile
 (fn [db [_ coordinate]]
   (-> db :ants (get coordinate))))

(re-frame/reg-sub
 :tile-state
 (fn [[_ coordinate] _]
   (re-frame/subscribe [:ant-at-tile coordinate]))
 (fn [tile-state _]
   tile-state))
