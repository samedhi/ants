(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]))

(defn component []
  [:ul {:id :grid :class :clear}
   (for [i (range 10)]
     ^{:key i}
     [:li
      [:div {:class :hexagon}]])])

