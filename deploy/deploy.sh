#!/usr/bin/env bash
set -e
set -o pipefail
export APP_NAME=twitter
export SECRETS=${APP_NAME}-secrets
export SECRETS_FN=$HOME/${SECRETS}
export IMAGE_NAME=gcr.io/${PROJECT_ID}/${APP_NAME}
export RESERVED_IP_NAME=${APP_NAME}-ip

echo "-----"
echo $RESERVED_IP_NAME
echo $IMAGE_NAME
echo $SECRETS_FN
echo $APP_NAME
echo "-----"


docker rmi -f $IMAGE_NAME || echo "couldn't delete the old image, $IMAGE_NAME. It doesn't exist."

cd $ROOT_DIR

./mvnw -DskipTests=true spring-javaformat:apply clean package spring-boot:build-image -Dspring-boot.build-image.imageName=$IMAGE_NAME
docker push $IMAGE_NAME

gcloud compute addresses list --format json | jq '.[].name' -r | grep $RESERVED_IP_NAME || gcloud compute addresses create $RESERVED_IP_NAME --global
touch "$SECRETS_FN"
echo writing to "$SECRETS_FN "
cat <<EOF >${SECRETS_FN}
SPRING_R2DBC_PASSWORD=${SPRING_R2DBC_PASSWORD}
SPRING_R2DBC_URL=${SPRING_R2DBC_URL}
SPRING_R2DBC_USERNAME=${SPRING_R2DBC_USERNAME}
SPRING_RABBITMQ_HOST=${SPRING_RABBITMQ_HOST}
SPRING_RABBITMQ_PASSWORD=${SPRING_RABBITMQ_PASSWORD}
SPRING_RABBITMQ_PORT=${SPRING_RABBITMQ_PORT}
SPRING_RABBITMQ_USERNAME=${SPRING_RABBITMQ_USERNAME}
SPRING_RABBITMQ_VIRTUAL_HOST=${SPRING_RABBITMQ_VIRTUAL_HOST}
TWITTER_APP_CLIENT_ID=${TWITTER_APP_CLIENT_ID}
TWITTER_APP_CLIENT_SECRET=${TWITTER_APP_CLIENT_SECRET}
TWITTER_CLIENTS_0_CLIENT_ID=${TWITTER_CLIENTS_0_CLIENT_ID}
TWITTER_CLIENTS_0_SECRET=${TWITTER_CLIENTS_0_SECRET}
TWITTER_CLIENTS_1_CLIENT_ID=${TWITTER_CLIENTS_1_CLIENT_ID}
TWITTER_CLIENTS_1_SECRET=${TWITTER_CLIENTS_1_SECRET}
TWITTER_ENCRYPTION_PASSWORD=${TWITTER_ENCRYPTION_PASSWORD}
TWITTER_ENCRYPTION_SALT=${TWITTER_ENCRYPTION_SALT}
SPRING_PROFILES_ACTIVE=production
EOF
kubectl delete secrets $SECRETS || echo "no secrets to delete."
kubectl create secret generic $SECRETS --from-env-file "$SECRETS_FN"
kubectl delete -f "$ROOT_DIR"/deploy/k8s/deployment.yaml || echo "couldn't delete the deployment as there was nothing deployed."
kubectl apply -f "$ROOT_DIR"/deploy/k8s
