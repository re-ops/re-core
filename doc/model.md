# Model

Celetial model is as generic as possible and is composed of:

* A machine is the virtualized/physical medium we work on.
* A type (for example a mysql server type) that effects what will be installed on the machine, a type can be binded to a module.
* module is the provision info require to setup this machine (a type points to module)
* host is the primary key in accessing all of these and is unique, a host has a type and a machine associated to it

