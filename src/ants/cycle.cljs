(ns ants.cycle
  (:require
   [cljs.core.async :as async]
   [re-frame.core :as re-frame])
  (:require-macros
   [cljs.core.async :as async]))

(defonce keep-the-go-loop-from-reinitializing
  (async/go-loop []
    (let [ms @(re-frame/subscribe [:time-between-ticks])
          c (async/timeout ms)]
      (re-frame/dispatch [:tick])
      (async/<! c)
      (recur))))
