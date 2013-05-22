(ns celestial.test.validations
  "validations tests"
  (:use 
    midje.sweet
    [proxmox.validations :only (validate-entity)]
    [celestial.fixtures :only (redis-prox-spec with-m?)])
  (:import clojure.lang.ExceptionInfo)
  )


(fact "proxmox entity validation"
   (validate-entity redis-prox-spec) => truthy

   (validate-entity (assoc-in redis-prox-spec [:machine :cpus] nil)) => ; common validation 
      (throws ExceptionInfo (with-m? {:machine {:cpus '("cpus must be present")}}))

   (validate-entity (assoc-in redis-prox-spec [:machine :os] "ubutnu-12.04")) => ; entity validation 
      (throws ExceptionInfo (with-m? {:machine {:os '("os must be a keyword")}}))

   (validate-entity (assoc-in redis-prox-spec [:proxmox :password] nil)) => ; proxmox validation
      (throws ExceptionInfo (with-m? {:proxmox {:password '("password must be present")}})))


