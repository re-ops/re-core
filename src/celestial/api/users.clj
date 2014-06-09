(ns celestial.api.users
  (:require
    [celestial.model :as m]
    [cemerick.friend.credentials :as creds]
    [celestial.persistency.users :as u]
    [celestial.persistency.quotas :as q]
    [cemerick.friend :as friend]
    [celestial.roles :refer (roles roles-m)]
    [clojure.core.strint :refer (<<)]
    [slingshot.slingshot :refer  [throw+ try+]]
    [swag.model :refer (defmodel wrap-swag defc)]
    [celestial.common :refer
     (import-logging resp bad-req conflict success wrap-errors)]
    [swag.core :refer 
     (swagger-routes GET- POST- PUT- DELETE- defroutes- errors)]
    ) 
  )

(defn convert-roles [user]
  (update-in user [:roles] (fn [v] (into #{} (map #(roles-m %) v)))))

(defn hash-pass [user]
  (update-in user [:password] (fn [v] (creds/hash-bcrypt v))))

(defn into-persisted
   "converting user to persisted version" 
   [{:keys [password envs operations] :as user}]
  (cond-> (-> user convert-roles) 
     (not (empty? envs)) (update-in [:envs] (partial mapv keyword))
     (not (empty? operations)) (update-in [:operations] (partial mapv keyword))
     (not (empty? password)) hash-pass
     ))

(defmodel user :username :string :password :string 
  :roles {:type :string :allowableValues {:valueType "LIST" :values (into [] (keys roles-m))}}
  :envs  {:type "List"})

(defroutes- users {:path "/user" :description "User managment"}

  (GET- "/users/roles" [] {:nickname "getRoles" :summary "Get all existing user roles"}
        (success {:roles roles-m}))

  (GET- "/users/:name" [^:string name] {:nickname "getUser" :summary "Get User"}
        (success (u/get-user name)))

  (POST- "/users" [& ^:user user] {:nickname "addUser" :summary "Adds a new user"}
         (wrap-errors (u/add-user (into-persisted user))
           (success {:message "added user"})))

  (PUT- "/users" [& ^:user user] {:nickname "updateUser" :summary "Updates an existing user"}
        (wrap-errors 
            (u/partial-user (into-persisted user))
            (success {:message "user updated"})))

  (DELETE- "/users/:name" [^:string name] {:nickname "deleteUser" :summary "Deleted a user"}
           (u/delete-user name) 
           (success {:message "user deleted" :name name})))

(defroutes- users-ro {:path "/user" :description "Read only User data"}

  (GET- "/users" [] {:nickname "getUsers" :summary "Get all users"}
        (success (map #(dissoc (u/get-user %) :password) (u/all-users))))
  
  (GET- "/users/operations" [] {:nickname "getUsersOperations" :summary "Get all available operations (not of a specific user)."}
        (success {:operations (map name m/operations)})))

(defroutes- users-current {:path "/users" :description "Logged in user info"}
  (GET- "/users/current/operations" [] 
      {:nickname "currentOperations" 
       :summary "Get current logged in user operations"}
 (success 
   (select-keys 
     (u/get-user (:username (friend/current-authentication))) [:operations]))))

(defmodel quota :username :string :quotas {:type :Quotas})

(defmodel quotas :env {:type :Env})

(defmodel env :hypervisor {:type :Limits})

(defmodel limits :limits {:type :Counts} :used {:type :Counts})

(defmodel counts :count :int)

(defroutes- quotas {:path "/quota" :description "User quota managment"}

  (GET- "/quotas" [] {:nickname "getQuotas" :summary "Get all quotas"}
    (success (map q/get-quota (q/all-quotas))))

  (GET- "/quotas/:name" [^:string name] {:nickname "getQuota" :summary "Get users quota"}
    (success (q/get-quota name)))

  (POST- "/quotas" [& ^:quota quota] {:nickname "addQuota" :summary "Adds a user quota"}
    (wrap-errors 
       (q/add-quota quota)
       (success {:message "added quota"})))

  (PUT- "/quotas" [& ^:quota quota] {:nickname "updateQuota" :summary "Updates an existing quota"}
    (wrap-errors 
      (q/update-quota quota)
      (success {:message "quota updated"})))

  (DELETE- "/quotas/:name" [^:string name] {:nickname "deleteQuota" :summary "Deleted users quota"}
     (q/delete-quota! name) 
     (success {:message "quota deleted"})))
