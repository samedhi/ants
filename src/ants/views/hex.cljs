(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]))

(defn component []
  [:div {:id :grid :class :clear}
   (for [row-i (range 1)]
     ^{:key row-i}
     [:div {:class :row}
      (for [col-i (range 10)]
        ^{:key (str "row-" row-i ":col-" col-i)}
        [:div {:class :column}
         [:li
          [:div {:class :hexagon}]]])])])

