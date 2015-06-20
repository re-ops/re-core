# Intro

Celestial is a provisioning server for reliable automated infra management, it enables you to integrate hypervisors, configuration managments and deployment tools into a chohesive, servicable systems.

For how to get started and more info please check [celestial-ops.com](http://celestial-ops.com)

[![Build Status](https://travis-ci.org/celestial-ops/celestial-core.png)](https://travis-ci.org/celestial-ops/celestial-core)

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
$ lein celestial
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

Copyright [2013] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
