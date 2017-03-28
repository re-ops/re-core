# Intro

re-core is a provisioning server and live environment for setting up cloud/vms instances in a consistent reliable way.

For how to get started and more info please check [this](https://celestial-ops.github.io/landing/)

[![Build Status](https://travis-ci.org/re-ops/re-core.png)](https://travis-ci.org/re-ops/re-core)

# Developing

First set a local redis instance:

```bash
$ git clone git://github.com/opskeleton/redis-sandbox.git
$ cd redis-sandbox
$ ./boot.sh
```

Launch a demo instance (create a matching ~/.celestial.edn):
```bash
$ lein populate
$ lein re-core
# check https://localhost:8443
```

For interactive development:

```bash
$ lein clean
$ lein dev-repl
user=> (go)
; in order to reload the entire app
user => (reset)
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
