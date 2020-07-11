(ns ants.views.util
  (:require
   [re-frame.core :as re-frame]
   [ants.config :as config]
   [ants.mui :as mui]
   [ants.subs :as subs]))

(defn code-block [o]
  [:pre {:class "code-block"}
   [:code o]])

(defn app-db-viewer []
  (let [db @(re-frame/subscribe [:pretty-print-db])]
    [mui/card
     {:style {:margin "1rem 0"}}
     [mui/card-content
      [mui/typography {:color "textSecondary"} "Content of app-db is:"]
      [code-block db]]]))

(defn footer []
  [mui/grid
   {:class "footer"}
   [mui/grid
    [mui/link {:href "https://samedhi.github.io/"} "Stephen Cagle"]
    [mui/link {:href "https://github.com/samedhi"} "@github"]
    [mui/link {:href "https://www.linkedin.com/in/stephen-cagle-92b895102/"} "@linkedin"]]
   [mui/typography "Copyright 2020"]])

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
