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

