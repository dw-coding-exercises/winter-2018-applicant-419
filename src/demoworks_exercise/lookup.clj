(ns demoworks-exercise.lookup
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; Like-a dis:
;; https://api.turbovote.org/elections/upcoming?district-divisions=ocd-division/country:us/state:al,ocd-division/country:us/state:al/place:birmingham'

;;----------------------------------Constants------------------------------------
(def api-root "https://api.turbovote.org")

;; TODO: always nice to have a more flexible way of putting together endpoints,
;; but this is fast.
(def query-endpoint "elections/upcoming")

(def ocd-division "ocd-division")
(def ocd-country "country:us")
(def ocd-base [ocd-division ocd-country])

;; Control structures; lets me only build OCD strings with keys I can presently
;; support.
;; The federal case is only the ocd-country string; hrm.
(def state-keys    [:state])
(def city-keys     [:state :place])
(def county-keys   [:state :county])
(def known-formats [state-keys city-keys county-keys])

;;------------------------------------Data--------------------------------------
;; Who doesn't love a consistently-shaped request?
;; TODO: Remember to render BOTH state and "more precise" (e.g. place) OCDs

(defrecord OCDEntry [street place state zip county district])

;; Do I want this?
;; {:keys [street city state zip country district]}
(defn render-ocd
  "Render a single OCD to a correctly formatted lookup string"
  [ocd-entry key-order]
  (let [pieces (into ocd-base
                     (for [k key-order
                           :let [ocd-val (k ocd-entry)
                                 k-name   (name k)]]
                       (str k-name ":" ocd-val)))]
    (str/join "/" pieces)))

(defn render-ocds
  "Render the list of OCDs into a correctly formatted query string"
  [ocd-entry]
  (let [ocd-strings (for [ks known-formats]
                      (render-ocd ocd-entry ks))]
    (str/join "," ocd-strings)))

;;----------------------------------Requests------------------------------------

(defn query-api
  [ocd-entry endpoint]
  (let [rendered-ocd (render-ocds ocd-entry)
        params       {"district-divisions" render-ocd}
        ;; There is every possibility that this is YAGNI, but it makes me feel
        ;; better about forward flexibility to admit we might query something
        ;; different eventually
        url          (str/join "/" [api-root endpoint])]
    (http/get url {:query-params params})))


;;-------------------------------Result Parsing---------------------------------

;; Design grump: oye can chasing down nils be a pain. With more time, I'd prefer
;; some kind of error object this could return -- something more like an Either.
(defn read-body
  "Ingests the result of querying the turbovote API. Tries to parse the result
  body as EDN; logs an error and continues if parsing fails.

  N.B. edn/read-string throws RuntimeException if it gets grumpy. Thx, Rich."
  [response-body]
  (try
    (edn/read-string response-body)
    (catch java.lang.RuntimeException rte
      (log/error "Couldn't parse response body:" response-body))))
