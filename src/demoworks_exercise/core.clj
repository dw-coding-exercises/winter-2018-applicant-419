(ns demoworks-exercise.core
  (:require [compojure.core :refer :all] ;; TODO: Tighten this down. Not super important, but tidy NSes are nice as heck.
            [compojure.route :as route]
            [demoworks-exercise.home :as home]
            demoworks-exercise.logging
            [demoworks-exercise.lookup :as lookup]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [taoensso.timbre :as log]))

(defroutes app
  (GET "/" [] home/page)
  (POST "/search" [:as {params :form-params}] (lookup/lookup params))
  (route/resources "/")
  (route/not-found "Not found"))

(def handler
  (-> app
      (wrap-defaults site-defaults)
      wrap-reload))

(log/info "We're up!")
