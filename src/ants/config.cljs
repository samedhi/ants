(ns ants.config)

(def breakpoints
  {:breakpoints [:mobile        768
                 :tablet        992
                 :small-monitor 1200
                 :large-monitor     ]
   :debounce-ms 166})

(def default-db {:q-max 3
                 :r-max 4})
