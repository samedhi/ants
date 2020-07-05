(ns ants.events
  (:require
   [re-frame.core :as re-frame]
   [ants.config :as config]))

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   config/default-db))

(re-frame/reg-event-db
 :tick
 (fn [db _]
   (println :tick)
   db))
