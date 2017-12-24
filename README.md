# Intro

Re-core is a live environment for setting up VM instances in a consistent and reliable way across multiple providers (AWS, DigitalOcean and Native KVM).

It is a part of the [re-ops](https://github.com/re-ops) project that offers a live coding environment for configuration management.

[![Build Status](https://travis-ci.org/re-ops/re-core.png)](https://travis-ci.org/re-ops/re-core)

# Get running

```clojure
# get ES running
$ sudo docker-compose up
$ git clone git@github.com:re-ops/re-mote.git
$ cd re-mote
# Now start the REPL environment
$ lein repl
[re-core]λ: (go)
nil
[re-core]λ: (list)
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
