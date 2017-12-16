(ns demoworks-exercise.logging
  (:require [clj-time.core :as time]
            [clj-time.format :as f]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as a]))

(defn make-log-name []
  (let [now (time/now)
        now-str (f/unparse (f/formatters :date) now)]
    (str now-str "_demoworks_server.log")))

(def logging-config
  {:timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss ZZ"}
   :appenders
   {:println (a/println-appender {:level :debug})
    :file (a/spit-appender {:async? true
                            :fname (make-log-name)})}})

;; Eeeeeeversoslightly janky, just straight eval'ing this, but we don't have a
;; more obvious place for it right now.

;; TODO: I recall ring servers having what amounts to an init hook for loading
;; configs on server start; I don't want to take the time to find it right now,
;; but this should /eventually/ be a function called by that hook, or similar
;; dont-eval-con-compile wiring.
(let [log-level :debug] ;; Hard-code for now
  (log/set-level! log-level)
  (log/merge-config! logging-config)
  (log/info "Logging configs set."))
