(ns celestial.users-api
  (:require
    [cemerick.friend.credentials :as creds]
    [celestial.persistency :as p]) 
  (:use 
    [celestial.roles :only (roles roles-m admin)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+ try+]]
    [swag.model :only (defmodel wrap-swag defv defc)]
    [celestial.common :only (import-logging resp bad-req conflict success)]
    [swag.core :only (swagger-routes GET- POST- PUT- DELETE- defroutes- errors)]))

(defn convert-roles [user]
  (update-in user [:roles] (fn [v] #{(roles-m v)})))

(defn hash-pass [user]
  (update-in user [:password] (fn [v] (creds/hash-bcrypt v))))
 
(defmodel user :username :string :password :string 
  :roles {:type :string :allowableValues {:valueType "LIST" :values (into [] (keys roles-m))}})

(defroutes- users {:path "/user" :description "User managment"}
  (GET- "/user/:name" [^:string name] {:nickname "getUser" :summary "Get User"}
        (success (p/get-user name)))

  (POST- "/user/" [& ^:user user] {:nickname "addUser" :summary "Adds a new user"}
         (p/add-user (-> user convert-roles hash-pass))
         (success {:msg "added user"}))

  (PUT- "/user/" [& ^:user user] {:nickname "updateUser" :summary "Updates an existing user"}
        (p/update-user (-> user convert-roles hash-pass))
        (success {:msg "user updated"}))

  (DELETE- "/user/:name" [^:string name] {:nickname "deleteUser" :summary "Deleted a user"}
           (p/delete-user name) 
           (success {:msg "user deleted" :name name})))
