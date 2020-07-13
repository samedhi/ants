(ns ants.config)

(def decay-rate 0.90)

(def max-pheromone 100)

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

(def even-row->facing->coordinate-delta
  {:even {:northeast [-1 1]
          :east [0 1]
          :southeast [1 1]
          :southwest [1 0]
          :west [0 -1]
          :northwest [-1 0]}
   :odd {:northeast [-1 0]
         :east [0 1]
         :southeast [1 0]
         :southwest [1 -1]
         :west [0 -1]
         :northwest [-1 -1]}})

(def default-ant {:facing :northeast :max-steps 30})

(def default-db {:selected-tool :drop-ant
                 :tick 0
                 :row-count 10
                 :column-count 10
                 :time-between-ticks 100;; milliseconds
                 :pheromones {}
                 :food {[6 7] 5000}
                 :entrences #{[1 1]}
                 :ants {[1 1] default-ant}})
