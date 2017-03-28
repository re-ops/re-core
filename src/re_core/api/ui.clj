(ns re-core.api.ui
  (:require
    [compojure.core :refer (defroutes GET ANY)] 
    [compojure.route :as route]   
    [clojure.core.strint :refer (<<)]
    [me.raynes.fs :as fs]
    [cemerick.friend :as friend]
    [re-core.common :refer (success)]
    [cemerick.friend.credentials :as creds]
    [swag.core :refer (swagger-routes GET- defroutes- errors)]))

(defn static-path []
  (let [cwd (System/getProperty "user.dir") parent "public/re-core-ui"
        build (<< "~{cwd}/~{parent}/build") bin (<< "~{cwd}/~{parent}/bin")]
    (if (fs/exists? build ) build bin)))

(defn static-path-2 [] "public/elm-ui/")

(defroutes- sessions {:path "/sessions" :description "Session info"} 
  (GET- "/sessions" [] {:nickname "currentSession" :summary "Get current logged in user info"}
     (success (friend/current-authentication))))

(defroutes public
  (GET "/login" [] (ring.util.response/file-response "login.html" {:root (static-path-2)}))
  (friend/logout (ANY "/logout" request  (ring.util.response/redirect "/")))
  (route/files "/" {:root (static-path-2)})
  (route/files "/old" {:root (static-path)})
  )
 

