(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]))

(defn component []
  [:ul {:id :grid :class :clear}
   [:li
    [:div {:class :hexagon}]]])

