# Intro 

Some Access control limits ideas thoughts:

What we need to limit?

* limit users to certain envs
* limit user to certain actions


Possible simple structure:

* Data: 
```clojure
 {:ronen :permissions {:env [:dev :test]}}
```

* Preds: 
```clojure
  (defn ^{:permission {:role :admin}} foo [])
```

Runtime check:

*  apply a check on each fn run? (f) -> permissions? 

* attach user to thread local -> check before fn
