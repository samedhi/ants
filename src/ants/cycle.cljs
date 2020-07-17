(ns ants.cycle
  (:require
   [cljs.core.async :as async]
   [re-frame.core :as re-frame])
  (:require-macros
   [cljs.core.async :as async]))

(defonce keep-the-go-loop-from-reinitializing
  (let [time-between-ticks (re-frame/subscribe [:time-between-ticks])]
    (async/go-loop []
      (let [work-complete-chan (async/chan)
            time-between-ticks-chan (async/timeout @time-between-ticks)]
        (re-frame/dispatch [:tick work-complete-chan])
        (async/<! work-complete-chan)
        (async/<! time-between-ticks-chan)
        (recur)))))
