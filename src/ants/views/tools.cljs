(ns ants.views.tools
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]
   [ants.mui :as mui]
   [ants.util :as util]
   [ants.views.images :as images]
   [ants.views.util :as views.util]))

(def tools
  [{:event :drop-ant
    :image-map images/ant-walk}
   {:event :drop-colony
    :image-map images/mound}
   {:event :drop-10-food
    :image-map (assoc images/ant-walk-with-food :row 7 :col 7)}
   {:event :drop-100-food
    :image-map (assoc images/ant-walk-with-food :row 7 :col 7)}
   {:event :drop-pheromone
    :image-map images/eyedropper}
   {:event :drop-10-pheromones
    :image-map images/eyedropper}
   ])

(defn tool-button [tool]
  (let [{:keys [event image-map text]} tool]
    [:div.button
     (string/replace (name event) "-" " ")]))

(defn component []
  [mui/paper {:class "tools"}
   [:div {:class "container"}
    (for [tool tools]
      ^{:key (-> tool :event name)}
      [tool-button tool])]])

