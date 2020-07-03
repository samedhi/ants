(ns ^:figwheel-hooks ants.core
  (:require
   [firemore.core :as firemore]
   [goog.dom :as goog.dom]
   [reagent.dom :as reagent.dom]
   [re-frame.core :as re-frame]
   [ants.breakpoints :as breakpoints]
   [ants.events :as events]
   [ants.subs :as subs]
   [ants.views.root :as root]))

(defn mount-root []
  (when-let [el (goog.dom/getElement "app")]
    (reagent.dom/render root/component el)))

(defn ^:after-load init []
  (re-frame/clear-subscription-cache!)
  (breakpoints/init)
  (mount-root)
  :success)

(defonce run-at-app-startup
  (do
    (re-frame/dispatch-sync [:initialize-db])
    (init)))
