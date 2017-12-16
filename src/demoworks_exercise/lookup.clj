(ns demoworks-exercise.lookup
  (:require [clj-http.client :as http]))

;; Like-a dis:
;; https://api.turbovote.org/elections/upcoming?district-divisions=ocd-division/country:us/state:al,ocd-division/country:us/state:al/place:birmingham'

;;----------------------------------Constants------------------------------------
(def api-root "https://api.turbovote.org/")

;; TODO: always nice to have a more flexible way of putting together endpoints,
;; but this is fast.
(def query-endpoint "elections/upcoming")

;;----------------------------------Requests------------------------------------


;;-------------------------------Result Parsing---------------------------------
