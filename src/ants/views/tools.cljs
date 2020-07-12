(ns ants.views.tools
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]
   [ants.mui :as mui]
   [ants.util :as util]
   [ants.views.images :as images]
   [ants.views.util :as views.util]))

(def tools
  [{:tool :drop-ant
    :image-map images/ant-walk}
   {:tool :drop-colony
    :image-map images/mound}
   {:tool :drop-10-food
    :image-map (assoc images/ant-walk-with-food :row 7 :column 7)}
   {:tool :drop-100-food
    :image-map (assoc images/ant-walk-with-food :row 7 :column 7)}
   {:tool :drop-pheromone
    :image-map images/eyedropper}
   {:tool :drop-10-pheromones
    :image-map images/eyedropper}
   ])

(defn tool-button [tool-map]

  (let [{:keys [tool image-map text]} tool-map
        selected-tool @(re-frame/subscribe [:selected-tool])]
    [:div.button
     (merge
      {:on-click
      (fn [evt] (re-frame/dispatch [:select-tool tool]))}
      (when (= tool selected-tool) {:class "selected"}))
     [mui/typography
      {:class "text"}
      (string/replace (name tool) "-" " ")]
     [:div.image-container
      [views.util/image
       image-map
       :none
       [(get image-map :row 0) (get image-map :column 0)]]]]))

(defn component []
  [:div.tools-center
   [mui/paper {:class "tools"}
    (for [tool-map tools]
      ^{:key (-> tool-map :tool name)}
      [tool-button tool-map])]])

