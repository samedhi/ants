(ns ants.config)

(def breakpoints
  {:breakpoints [:mobile        768
                 :tablet        992
                 :small-monitor 1200
                 :large-monitor     ]
   :debounce-ms 166})

(def facing->degrees
  {:northeast 30
   :east 90
   :southeast 150
   :southwest 210
   :west 270
   :northwest 330})

(def default-db {:row-count 3
                 :column-count 5
                 :ants {[2 2] {:facing :northeast}}})
