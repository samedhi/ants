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
 :q-max
 (fn [db]
   (:q-max db)))

(re-frame/reg-sub
 :rows-count
 :<- [:q-max]
 (fn [q-max]
   (dec (* 2 q-max))))

(re-frame/reg-sub
 :r-max
 (fn [db]
   (:r-max db)))

(re-frame/reg-sub
 :columns-count
 :<- [:r-max]
 (fn [r-max]
   (dec (* 2 r-max))))
