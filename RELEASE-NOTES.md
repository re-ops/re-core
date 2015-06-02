# Celestial 0.9.2 ():

## Fixes:

* Fixing puppet provisioner empty yaml issue.

# Celestial 0.9.1 (31/05/15):

Fixing out of date UI

# Celestial 0.9.0 (28/05/15):

## Fixes:

## New Features
* Enabling environment in types, this removes a lot of duplication when defining types for multiple envs.

# Celestial 0.8.6 (25/05/15):

Moving to Clojure 1.6 with puny/carmine/friend update

# Celestial 0.8.5 (20/05/15):

## Fixes:
* Missing networks listing in Openstack view.

## New Features
* Adding Openstack scheduler hint support.
* Initial Openstack GC implementation.

# Celestial 0.8.4 (14/05/15):

## Fixes:

## New Features

* Consul hook support.

# Celestial 0.8.3 (10/05/15):

## Fixes:

## New Features
* Supporting Kibana 4 in job links.
* Making worker count per job configurable.

# Celestial 0.8.2 (30/04/15):

## Fixes:

* Fixing Openstack add UI (flavors listing not updating).
* Fixing Openstack instance view.
* Removing dashes from tid uuid making it more ES search friendly.

# Celestial 0.8.1 (31/03/15):
## Fixes:

* Fixing openstack security-groups 
* Fixing Openstack hardcoded key-name

## New Features


# Celestial 0.8.0 (29/03/15):
## Fixes:

## New Features

* Openstack initial support.

# Celestial 0.7.6 (21/12/14):
## Fixes:

* Fixing missing hostname in done jobs
* Fixing deleted systems hostname in jobs

# Celestial 0.7.4 (20/12/14):
## New Features:


* Adding proxmox task-timeout configuration option, this enables to set the timeout of all proxmox tasks (such as create, destroy etc..).
* Listing hostname under jobs view and shortening date format.

# Celestial 0.7.3 (22/11/14):
## New Features:

* Adding jobs reset on start/stop option.

# Celestial 0.7.2 (20/11/14):

## New Features:

* Adding jobs reset api call.

# Celestial 0.7.1 (29/09/14):

## Fixes:

* Fixing default fn in tinymasq update-dns hook fn.

# Celestial 0.7.0 (29/09/14):

## Fixes:

## New Features:
* Supporting [tinymasq](https://github.com/narkisr/tinymasq) tinymasq as a dns server using a new hook.

# Celestial 0.6.10 (14/08/14):

## Fixes:
* Fixing EBS volume validations (using composite validations with latest substantiation library).
* Moving to GELF 1.1 fixing Elastic facility parse error in the process (using latest gelfino-client library).

## New Features:


# Celestial 0.6.9 (4/08/14):

## Fixes:
* Fixing Cloning (both UI and backend).

## New Features:
* Supporting EBS optimized instances.
* Supporting EBS types.

# Celestial 0.6.8 (02/07/14):

## Fixes:
* Enabling secure http headers and session timeout.

## New Features:
* Adding m3/c3/r3 AWS instance type to the UI.
* Adding on boot startup option for Proxmox.

# Celestial 0.6.7 (10/06/14):
## Fixes:
* Fixed basic user operation listing by introducing /users/

## New Features:
* Re-indexing API call the enables an admin to relload all systems back into Elasticsearch.

# Celestial 0.6.6 (06/06/14):
## Fixes:
* Fixing add in the UI.

# Celestial 0.6.5 (01/06/14):
## Fixes:
* Passing integer to system deletion fixing deletion in Elasticsearch aspect.
* Flushing to Elasticsearch on each put/delete, making system API calls consistent.

## New Features:


# Celestial 0.6.4 (28/05/14):
## Fixes:

## New Features:
* Done jobs are persisted into Elasticsearch, pagination through history of jobs is now possible (old jobs are kept up to a configurable ttl).

# Celestial 0.6.3 (11/05/14):
## Fixes:

## New Features:

* Adding allowed operations per user, operations are filtered in the UI as well.
* Seperated action list in the UI clearly listing when no systems are selected.

# Celestial 0.6.2 (04/05/14):
## Fixes:
* Supporting dashes in search terms.
* Supporting wildcard queries.

## New Features:

# Celestial 0.6.1 (19/04/14):

## Fixes:
* Perm gen jvm parameter passing corrected.
* Default configuration template includes ES settings.

## New Features:
* Systems UI supports selecting multiple systems and launching actions/operations on them.
* Dynamic system count in view.
* Select / Clear all selection of systems.


# Celestial 0.6.0 (19/04/14):

## Fixes:
* Fixing agent restart on dns hook handler (fixing cases where the agent fails on non hooks exceptions).
* User add UI fix.
* Swag conversions fix.

## New Features:
* Added support for searching machines using [Elasticsearch](http://www.elasticsearch.org/) on systems both in the backend and front end.

# Celestial 0.5.1 (12/03/14):

## Fixes:
* Systems listing now follows URL.
* Quotas fixed and UI working.
* Owner in systems listing.
* Fixed AWS reload.


## New Features:
* Docker hypervisor [support](http://celestial-ops.com/posts/docker.html).
* New  [audits](http://celestial-ops.com/posts/audits.html) section.
* Operations menu on single system view.
* Clone an existing system (removing unique identifiers and hostname).
* Run Stage or create automatically after creating a new system in the UI.

