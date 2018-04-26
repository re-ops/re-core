# Intro

Re-core is a live functional environment for setting up VM instances in a consistent and reliable way across multiple providers (AWS, DigitalOcean and Native KVM).

It is a part of the [Re-ops](https://re-ops.github.io/re-ops/) project that offers a live coding environment for configuration management.

[![Build Status](https://travis-ci.org/re-ops/re-core.png)](https://travis-ci.org/re-ops/re-core)

# Get running

```clojure
$ git clone git@github.com:re-ops/re-core.git
$ cd re-core
# Now start the REPL environment
$ lein repl
[re-core]λ: (go)
nil
```

For more detailed information please follow the [setup](https://re-ops.github.io/re-docs/setup/re-core.html#intro) guide.

# Basic usage

We can create and manipulate VMs quickly from the repl, a fluent functional DLS enables us to have rapid provisioning:

[re-mote-gif](https://re-ops.github.io/re-one/gifs/re-core.gif)

For more information on how to use Re-core follow the [usage](https://re-ops.github.io/re-docs/usage/#re-core) guide.

# Copyright and license

Copyright [2018] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
