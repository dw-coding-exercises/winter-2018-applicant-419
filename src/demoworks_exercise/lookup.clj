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
(def known-formats [state-keys city-keys])

;; Form key values; give 'em a consistent place to update in case they change
(def form-state-key "state")
(def form-city-key "city")
(def form-street-one-key "street")
(def form-street-two-key "street-2")
(def form-zipcode-key "zip")

;;----------------------------------Datetimes------------------------------------
(defn format-dt
  "Format an instance of datetime as a nice string."
  [dt]
  (f/unparse (f/formatters :date-time) (c/from-date dt)))

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
;; Including parsing request data into something we can used
(defn clean-city-name
  "Take a city name, make it lower case, and replace spaces with underbars"
  [city-name]
  (-> city-name
      (str/lower-case)
      (str/replace " " "_")))

(defn parse-form-params-to-entry
  "Convert incoming form data to an OCDEntry, cleaning up fields as needed along
  the way."
  [form-params]
  (let [street (form-params form-street-one-key)
        city   (clean-city-name (form-params form-city-key))
        state  (str/lower-case (form-params form-state-key))
        zip    (form-params form-zipcode-key)]
    (->OCDEntry street city state zip nil nil)))

(defn query-api
  "Talk to the TurboVote API; returns the entire response for downstream processing.

  Like-a dis:
  https://api.turbovote.org/elections/upcoming?district-divisions=ocd-division/country:us/state:al,ocd-division/country:us/state:al/place:birmingham'"
  [ocd-entry endpoint]
  (log/debug "Attempting API lookup on:" ocd-entry)
  (let [rendered-ocd (render-ocds ocd-entry)
        params       {"district-divisions" rendered-ocd}
        ;; There is every possibility that this is YAGNI, but it makes me feel
        ;; better about forward flexibility to admit we might query something
        ;; different eventually
        url          (str/join "/" [api-root endpoint])]
    (log/debug "Querying for OCD string:" rendered-ocd)
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

;;-------------------------Rendering things for Humans---------------------------
;; Gotta show people something!

;; TODO: refactor out duplicated pieces as makes sense
;; TODO: all of this can be tested to assure it renders well, but I am running low on time.

;; AHEM I'll just copy paste this shall I
(defn header []
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Find my next election"]
   [:link {:rel "stylesheet" :href "default.css"}]])

(defn render-voting-method [voting-method]
  [:div
   [:p (str "This " (if (:primary voting-method) "is" "is not") " a primary election.")]
   (case (:type voting-method)
     :in-person [:p (:instructions voting-method)]
     :by-mail [:p (str "For by-mail voting, your ballot request must be received by "
                       (format-dt (:ballot-request-deadline-received voting-method)))])])

(defn render-voting-methods [voting-methods]
  [:div (for [vm voting-methods]
          (render-voting-method vm))])

(defn render-voter-registration-instructions
  "Conditionally return those instruction keys I know of so far."
  [instructions]
  (for [k [:registration :signature :idnumber]]
    (if-let [v (instructions k)]
      [:p v])))

(defn render-voter-registration-method [vrm]
  [:div (str "For " (name (:type vrm)) " voting:")
   (render-voter-registration-instructions (:instructions vrm))
   (if-let [website (:url vrm)]
     [:p (str "Official website: " website)])
   (if-let [dt (:deadline-postmarked vrm)]
     [:p (str "Registration must be postmarked by " (format-dt dt))])])

(defn render-voter-registration-methods [voter-registration-methods]
  [:div
   [:p "To register to vote, you can use these methods:"]
   (for [vrm voter-registration-methods]
     (render-voter-registration-method vrm))])

(defn render-district-division [{:keys [voting-methods voter-registration-methods] :as division}]
  [:div
   [:p (str "This is a " (name (:election-authority-level division)) " election.")]
   (render-voting-methods voting-methods)
   (render-voter-registration-methods voter-registration-methods)])

(defn render-district-divisions [divisions]
  [:div
   (for [divis divisions]
     (render-district-division divis))])

(defn render-election [election]
  (log/debug election)
  (log/debug (type election))
  (log/debug (:description election))
  (log/debug (:website election))
  (log/debug (:date election))
  (let [{:keys [description website date]} election
        divisions (:district-divisions election)]
    [:div
     [:h1 description]
     [:h3 (str "To be held on: " (format-dt date))]
     [:h3 (str "Official website: " website)]
     (render-district-divisions divisions)]))

(defn render-elections [elections]
  (for [election elections] (render-election election)))

(defn render-results
  "Render upcoming elections into something a real human might ever be able to
  read :P"
  [query-result]
  (html5
   (header)
   (render-elections query-result)
   ))

(defn not-found [msg ocd-entry]
  (log/debug "Not found it. Blah. Telling the user about it.")
  (html5 [:div msg
          [:ul
           [:li (:street ocd-entry)]
           [:li (:place ocd-entry)]
           [:li (:state ocd-entry)]
           [:li (:zip ocd-entry)]]]))

;;------------------------Looking Things Up, For Realz--------------------------
;; The function we'll hand to our Compojure routes, back in core.clj

(defn lookup
  "Do a lookup! Hopefully. If it fails, be polite about it.

  Remember: form params are already parsed into a map with strings for keys, so
  we can't destructure this one."
  [form-params]
  (log/debug "Parsing lookup request for:" form-params)
  ;; TODO: This is where we start augmenting
  (let [ocd-entry (parse-form-params-to-entry form-params)
        response  (query-api ocd-entry query-endpoint)]

    ;; Make sure we check common status ailments
    ;; TODO: check statuses a lot more thoroughly
    (case (:status response)
      200 (if-let [parsed-edn (read-body (:body response))]
            (if (empty? parsed-edn)
              (not-found "No upcoming elections for this location.")
              (do
                (log/info "Got some stuff! Showing it to the user:")
                (render-results parsed-edn))))
      (not-found "The API returned a non-200 status code for this query:" ocd-entry))))
