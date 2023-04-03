#/bin/sh

REALM=oss
CONTAINER_ID=$(docker ps -q --filter ancestor=quay.io/keycloak/keycloak:21.0.0)

docker exec -e JDBC_PARAMS='?useSSL=false'  -ti $CONTAINER_ID /opt/keycloak/bin/kc.sh export --file /tmp/my_realm.json --realm $REALM
docker cp $CONTAINER_ID:/tmp/my_realm.json ../src/test/resources/kc.json