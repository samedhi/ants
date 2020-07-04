(ns ants.views.hex
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]
   [ants.views.images :as images]))

(defn image-portion [image-map row column]
  (let [{:keys [src width height rows columns]} image-map
        section-width (/ width columns)
        section-height (/ height rows)]
    [:div {:class :img-container}
     [:img {:src src
            :style {:position :relative
                    :top (str "-" row "00%")
                    :left (str "-" column "00%")
                    :height (str rows "00%")
                    :width (str columns "00%")
                    }}]]))

(defn hex [x y]
  (let []
    [:div {:class :column}
     [:li
      [:div {:class :hexagon}
       [image-portion images/ant-walk x y]]]]))

(defn component []
  (let [row-count 5
        column-count 5]
    [:div {:id :grid :class :clear}
     (for [row-i (range row-count)]
       ^{:key row-i}
       [:div {:class :row
              :style (merge
                      (when (pos? row-i) {:margin-top (str (/ -100 column-count 7.5) "%")})
                      (when (odd? row-i) {:right (str (/ 100 column-count 2) "%")}))}
        (for [col-i (range column-count)]
          ^{:key (str "row-" row-i ":col-" col-i)}
          [hex row-i col-i])])]))

