(ns ants.config
  (:require [re-frame.core :as re-frame]))

(def min-magnitude 256)

(def breakpoints
  {:breakpoints [:mobile        768
                 :tablet        992
                 :small-monitor 1200
                 :large-monitor     ]
   :debounce-ms 166})

(def facing->reverse-facing
  {:northeast :southwest
   :east :west
   :southeast :northwest
   :southwest :northeast
   :west :east
   :northwest :southeast})

(def facings
  (set (keys facing->reverse-facing)))

(def facing->degrees
  {:none 0
   :northeast 30
   :east 90
   :southeast 150
   :southwest 210
   :west 270
   :northwest 330})

(def facing->potentials
  {:northeast [:northwest :northeast :east]
   :east [:northeast :east :southeast]
   :southeast [:east :southeast :southwest]
   :southwest [:southeast :southwest :west]
   :west [:southwest :west :northwest]
   :northwest [:west :northwest :northeast]})

(def even-row?->facing->coordinate-delta
  {true  {:northeast [-1 1]
          :east [0 1]
          :southeast [1 1]
          :southwest [1 0]
          :west [0 -1]
          :northwest [-1 0]}
   false {:northeast [-1 0]
          :east [0 1]
          :southeast [1 0]
          :southwest [1 -1]
          :west [0 -1]
          :northwest [-1 -1]}})

(def default-ant {:facing :northeast
                  :max-steps 30
                  :name "ant-1"
                  :has-food? false})

(def default-db {:tile-magnitude 1
                 :selected-tool :drop-ant
                 :tick 0
                 :row-count 5
                 :column-count 5
                 :time-between-ticks 1000 ;; milliseconds
                 :pheromones-meta {:food {:decay-rate 0.97 :magnitude 100}
                                   :path {:decay-rate 0.99 :magnitude 5}}
                 :pheromones {}
                 :food {[3 3] 5000}
                 :entrences #{[1 1]}
                 :ants {[1 1] default-ant}})

(re-frame/dispatch [:merge-db (dissoc default-db :tick :pheromones :food :entrences :ants)])
