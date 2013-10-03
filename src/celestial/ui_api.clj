(ns celestial.ui-api
  (:require
    [compojure.core :refer (defroutes GET ANY)] 
    [compojure.route :as route]   
    [clojure.core.strint :refer (<<)]
    [me.raynes.fs :as fs]
    [cemerick.friend :as friend]
    [celestial.common :refer (success)]
    [cemerick.friend.credentials :as creds]
    [swag.core :refer (swagger-routes GET- defroutes- errors)]))

(defn static-path []
  (let [cwd (System/getProperty "user.dir") parent "public/celestial-ui"
        build (<< "~{cwd}/~{parent}/build") bin (<< "~{cwd}/~{parent}/bin")]
    (if (fs/exists? build ) build bin)))

(defroutes- sessions {:path "/sessions" :description "Session info"} 
  (GET- "/sessions" [] {:nickname "currentSession" :summary "Get current logged in user info"}
     (success (friend/current-authentication))))

(defroutes public
  (GET "/login" [] (ring.util.response/file-response "assets/login.html" {:root (static-path)}))
  (friend/logout (ANY "/logout" request  (ring.util.response/redirect "/")))
  (route/files "/" {:root (static-path)}) 
  )
 
