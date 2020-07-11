(ns ants.views.root
  (:require
   [re-frame.core :as re-frame]
   [ants.mui :as mui]
   [ants.views.hex :as hex]
   [ants.views.tools :as tools]
   [ants.views.util :as views.util]
   [ants.util :as util]))

(defn title []
  (let [greeting @(re-frame/subscribe [:greeting])]
    [mui/typography
     {:variant :h2
      :align :center}
     greeting]))

(defn component []
  [mui/container
   {:max-width "xl"}
   [title]
   [tools/component]
   [hex/component]
   [views.util/app-db-viewer]
   [views.util/footer]])
