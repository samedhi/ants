(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.config :as config]
   [ants.mui :as mui]
   [ants.views.images :as images]
   [ants.util :as util]
   [ants.views.util :as views.util]))

(defn tile [coordinate]
  (let [tile-state @(re-frame/subscribe [:tile-state coordinate])
        {:keys [facing entrence? food has-food?
                food-pheromone food-pheromone-opacity
                path-pheromone path-pheromone-opacity] :as state} tile-state]
    [:div {:class :column
           :on-click #(re-frame/dispatch [:tile-clicked coordinate])}
     [:li
      [:div {:class (conj [:hexagon] (when (pos? food) :food))}
       [:div {:style {:position :absolute
                      :width "100%"
                      :height "100%"
                      :display :flex
                      :align-items :center
                      :justify-content :center
                      :flex-direction :column}}
        [mui/typography (.toFixed food-pheromone 2)]
        [mui/typography (.toFixed path-pheromone 2)]]
       [:div {:style {:position :absolute
                      :width "100%"
                      :height "50%"
                      :background-color :red
                      :opacity (if (pos? food) 0 food-pheromone-opacity)}}]
       [:div {:style {:position :absolute
                      :top "50%"
                      :width "100%"
                      :height "100%"
                      :background-color :blue
                      :opacity (if (pos? food) 0 path-pheromone-opacity)}}]
       (when entrence?
         [:div {:style {:position :absolute
                        :width "100%"
                        :height "100%"}}
          [views.util/image images/mound]])
       (when (pos? food)
         [:div {:style {:position :absolute
                        :width "100%"
                        :height "100%"
                        :display :flex
                        :align-items :center
                        :justify-content :center}}
          ;; [views.util/image images/mound]
          [mui/typography {:variant :h4} food]])
       (when facing
         (let [[x y] coordinate
               ant-image (if has-food? images/ant-walk-with-food images/ant-walk)]
           [views.util/image ant-image facing [(mod x 7) (mod y 8)]]))]]]))

(defn component []
  (let [row-count @(re-frame/subscribe [:row-count])
        column-count @(re-frame/subscribe [:column-count])]
    [:div {:id :grid
           :class "hex-grid"
           :style {:padding-left (str (/ 100 column-count 2 ) "%")}}
     (for [row-i (range row-count)]
       ^{:key row-i}
       [:div {:class :row
              :style (merge
                      (when (pos? row-i) {:margin-top (str (/ -100 column-count 7.5) "%")})
                      (when (odd? row-i) {:right (str (/ 100 column-count 2) "%")}))}
        (for [col-i (range column-count)]
          ^{:key (str "row-" row-i ":col-" col-i)}
          [tile [row-i col-i]])])]))

