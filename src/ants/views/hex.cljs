(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.config :as config]
   [ants.mui :as mui]
   [ants.views.images :as images]))

(defn image
  ([image-map]
   (image image-map :none))
  ([image-map facing]
   (image image-map facing [0 0]))
  ([image-map facing coordinate]
   (let [[row column] coordinate
         {:keys [src width height rows columns] :or {rows 1 columns 1}} image-map
         section-width (/ width columns)
         section-height (/ height rows)]
     [:div {:class :img-container
            :style {:transform (str "rotate(" (config/facing->degrees facing) "deg)")}}
      [:img {:src src
             :style {:position :relative
                     :top (str "-" row "00%")
                     :left (str "-" column "00%")
                     :height (str rows "00%")
                     :width (str columns "00%")}}]])))

(defn tile [coordinate]
  (let [tile-state @(re-frame/subscribe [:tile-state coordinate])
        {:keys [facing entrance?] :as state} tile-state]
    [:div {:class :column}
     [:li
      [:div {:class :hexagon}
       (when entrance?
         [:div {:style {:position :absolute
                        :width "100%"
                        :height "100%"}}
          [image images/mound]])
       (when facing
         (let [[x y] coordinate]
           [image images/ant-walk facing [(mod x 7) (mod y 8)]]))]]]))

(defn component []
  (let [row-count @(re-frame/subscribe [:row-count])
        column-count @(re-frame/subscribe [:column-count])]
    [:div {:id :grid :class :clear}
     (for [row-i (range row-count)]
       ^{:key row-i}
       [:div {:class :row
              :style (merge
                      (when (pos? row-i) {:margin-top (str (/ -100 column-count 7.5) "%")})
                      (when (odd? row-i) {:right (str (/ 100 column-count 2) "%")}))}
        (for [col-i (range column-count)]
          ^{:key (str "row-" row-i ":col-" col-i)}
          [tile [row-i col-i]])])]))

