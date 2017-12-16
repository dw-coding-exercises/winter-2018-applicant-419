(ns demoworks-exercise.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [demoworks-exercise.home :as home]
            demoworks-exercise.logging
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [taoensso.timbre :as log]))

(defroutes app
  (GET "/" [] home/page)
  (route/resources "/")
  (route/not-found "Not found"))

(def handler
  (-> app
      (wrap-defaults site-defaults)
      wrap-reload))

(log/info "We're up!")
