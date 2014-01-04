(ns celestial.api.users
  (:require
    [cemerick.friend.credentials :as creds]
    [celestial.persistency :as p]) 
  (:use 
    [celestial.roles :only (roles roles-m)]
    [clojure.core.strint :only (<<)]
    [slingshot.slingshot :only  [throw+ try+]]
    [swag.model :only (defmodel wrap-swag defc)]
    [celestial.common :only (import-logging resp bad-req conflict success wrap-errors)]
    [swag.core :only (swagger-routes GET- POST- PUT- DELETE- defroutes- errors)]))

(defn convert-roles [user]
  (update-in user [:roles] (fn [v] (into #{} (map #(roles-m %) v)))))

(defn hash-pass [user]
  (update-in user [:password] (fn [v] (creds/hash-bcrypt v))))

(defn into-persisted
   "converting user to persisted version" 
   [{:keys [password envs] :as user}]
  (cond-> (-> user convert-roles) 
     (not (empty? envs)) (update-in [:envs] (partial mapv keyword))
     (not (empty? password)) hash-pass
     ))

(defmodel user :username :string :password :string 
  :roles {:type :string :allowableValues {:valueType "LIST" :values (into [] (keys roles-m))}}
  :envs  {:type "List"})

(defroutes- users {:path "/user" :description "User managment"}

  (GET- "/users/roles" [] {:nickname "getRoles" :summary "Get all existing user roles"}
        (success {:roles roles-m}))

  (GET- "/users/:name" [^:string name] {:nickname "getUser" :summary "Get User"}
        (success (p/get-user name)))

  (POST- "/users" [& ^:user user] {:nickname "addUser" :summary "Adds a new user"}
         (wrap-errors (p/add-user (into-persisted user))
           (success {:message "added user"})))

  (PUT- "/users" [& ^:user user] {:nickname "updateUser" :summary "Updates an existing user"}
        (wrap-errors 
            (p/partial-user (into-persisted user))
            (success {:message "user updated"})))

  (DELETE- "/users/:name" [^:string name] {:nickname "deleteUser" :summary "Deleted a user"}
           (p/delete-user name) 
           (success {:message "user deleted" :name name})))

(defroutes- users-ro {:path "/user" :description "Read only User data"}

  (GET- "/users" [] {:nickname "getUsers" :summary "Get all users"}
        (success (map #(dissoc (p/get-user %) :password) (p/all-users)))))

(defmodel quota :username :string :quotas {:type :Quotas})

(defmodel quotas :proxmox {:type :Limit})

(defmodel limit :limit :int)

(defroutes- quotas {:path "/quota" :description "User quota managment"}
  (GET- "/quotas/:name" [^:string name] {:nickname "getQuota" :summary "Get users quota"}
    (success (p/get-quota! name)))

  (POST- "/quotas/" [& ^:quota quota] {:nickname "addQuota" :summary "Adds a user quota"}
    (wrap-errors 
       (p/add-quota quota)
       (success {:message "added quota"})))

  (PUT- "/quotas/" [& ^:quota quota] {:nickname "updateQuota" :summary "Updates an existing quota"}
    (wrap-errors 
      (p/update-quota quota)
      (success {:message "quota updated"})))

  (DELETE- "/quotas/:name" [^:string name] {:nickname "deleteQuota" :summary "Deleted users quota"}
     (p/delete-quota! name) 
     (success {:message "quota deleted"})))
