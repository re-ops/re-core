. ./scripts/creds.sh
curl -X GET --user $AUTH -H "Accept: application/json; charset=UTF-8" http://localhost:8082/registry/host/machine/$1
