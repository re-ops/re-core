(ns celestial.roles
  "celetial roles"
 )

(def ^{:doc "roles string to keyword map"}
  roles-m {"admin" ::admin "user" ::user "anonymous" ::anonymous})

(def roles (into #{} (vals roles-m)))

(derive ::admin ::user)

(def user #{::user})

(def admin #{::user})
 
