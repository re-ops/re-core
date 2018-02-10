# Intro

Re-core is a live environment for setting up VM instances in a consistent and reliable way across multiple providers (AWS, DigitalOcean and Native KVM).

It is a part of the [re-ops](https://github.com/re-ops) project that offers a live coding environment for configuration management.

[![Build Status](https://travis-ci.org/re-ops/re-core.png)](https://travis-ci.org/re-ops/re-core)

# Get running

```clojure
# get ES running
$ sudo docker-compose up
$ git clone git@github.com:re-ops/re-core.git
$ cd re-core
# Now start the REPL environment
$ lein repl
[re-core]λ: (go)
nil
```


We can create and manipulate VM's quickly from the repl, a fluent functional DLS enables us to have rapid provisioning:

```clojure

[re-core]λ: (create kvm-small vol-128G :redis 5) ; Create 5 small redis instances with a 100G Volume

Running create summary:

  ✔ redis-a12af7a0d2
  ✔ redis-d55950759f
  ✔ redis-a9347f072f
  ✔ redis-42bd6672e3
  ✔ redis-7165e5c87b
```

Once created each system has a unique id attached to it, the systems are persisted into Elasticsearch.

We can inspect and operate on instances:

```clojure
[re-core]λ: (list)
              redis-d55950759f  AWF_17ZSdVoDKuXB7mtt       redis     ubuntu-16.04  192.168.122.142
              redis-7165e5c87b  AWF_17bgdVoDKuXB7mt9       redis     ubuntu-16.04  192.168.122.209
              redis-a12af7a0d2  AWF_17X2dVoDKuXB7mtl       redis     ubuntu-16.04  192.168.122.147
              redis-42bd6672e3  AWF_17asdVoDKuXB7mt4       redis     ubuntu-16.04  192.168.122.196
              redis-a9347f072f  AWF_17aCdVoDKuXB7mt0       redis     ubuntu-16.04  192.168.122.14


[re-core]λ: (halt)

Running halt summary:

  ✔ redis-d55950759f
  ✔ redis-7165e5c87b
  ✔ redis-a12af7a0d2
  ✔ redis-42bd6672e3
  ✔ redis-a9347f072f

[re-core]λ: (start) ; start all

Running start summary:

  ✔ redis-d55950759f
  ✔ redis-7165e5c87b
  ✔ redis-a12af7a0d2
  ✔ redis-42bd6672e3
  ✔ redis-a9347f072f

[re-core]λ: (provision) ; puppet provision instances using re-mote
```

Operations take a filtering function, making it real easy to partition and segement the instances we operate on:

```clojure
[re-core]λ: (halt (by-type :redis))

Running halt summary:

  ✔ redis-d55950759f
  ✔ redis-7165e5c87b
  ✔ redis-a12af7a0d2
  ✔ redis-42bd6672e3
  ✔ redis-a9347f072f

[re-core]λ: (start (matching "B7mtt")) ; select instance by SHA partial matching (git style)

Running halt summary:

  ✔ redis-d55950759f
```

Converting systems into re-mote hosts enable us to perform any re-mote operation:

```clojure
[re-core]λ: (hosts)

#re_mote.repl.base.Hosts {:auth {:user "re-ops"} :hosts ["192.168.122.226" "192.168.122.202" "192.168.122.159" "192.168.122.161" "192.168.122.174"]}

[re-core]λ: (updata (hosts)) ; apt update all the hosts
```


# Copyright and license

Copyright [2017] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
