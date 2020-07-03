(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]))

(defn component []
  (let [row-count 10
        column-count 19]
    [:div {:id :grid :class :clear}
     (for [row-i (range row-count)]
       ^{:key row-i}
       [:div {:class :row
              :style (merge
                      (when (pos? row-i) {:margin-top (str (/ -100 column-count 7.5) "%")})
                      (when (odd? row-i) {:right (str (/ 100 column-count 2) "%")}))}
        (for [col-i (range column-count)]
          ^{:key (str "row-" row-i ":col-" col-i)}
          [:div {:class :column}
           [:li
            [:div {:class :hexagon}]]])])]))

