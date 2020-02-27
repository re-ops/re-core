# Intro

This repository includes two main components:

* Re-core is a live functional environment for setting up VM instances in a consistent and reliable way across multiple providers (AWS, DigitalOcean,KVM and LXC).

* Re-mote a remote management swiss knife that supports SSH and ZeroMQ (using [Re-gent](https://github.com/re-ops/re-gent)) enabling provisioning, security auditing and metric collection using distributed Clojure functions.

Both components are a part of the [Re-ops](https://re-ops.github.io/re-ops/) project that offers a live coding environment for configuration management.

For more detailed information please follow the [setup](https://re-ops.github.io/re-docs/setup/) and [usage](https://re-ops.github.io/re-docs/usage/) guides.

[![Build Status](https://travis-ci.org/re-ops/re-core.png)](https://travis-ci.org/re-ops/re-core)

# Basic usage

Re-core enables us to create and manipulate VMs quickly from the REPL using a fast functional DSL for rapid provisioning:

![re-core-gif](https://re-ops.github.io/re-one/gifs/re-core.gif)

# Copyright and license

Copyright [2020] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
