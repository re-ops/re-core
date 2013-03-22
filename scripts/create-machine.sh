. ./scripts/creds.sh

curl -X POST --user $AUTH http://localhost:8082/machine/$1
