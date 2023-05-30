# Deploying a query node as a serverless function

## Login to ECS

Requires that there's an `orbital` profile defined in `~/.aws/credentials`:

```
[orbital]
aws_access_key_id=xxx
aws_secret_access_key=xxxx
```

Then:

```bash
aws ecr get-login-password --region eu-west-2 --profile orbital | docker login --username AWS --password-stdin 801563263500.dkr.ecr.eu-west-2.amazonaws.com
```

## Build native image

The below assumes we're pushing to the Orbital ECS store.
If using a customer specific private ECS store, update accordingly

* Generally, perform a `mvn clean install` (or `mvnd ...`) on the full project before doing this.

```bash
cd query-node-native
# This takes a long time execute - 2- 3 minutes
mvn clean -Pnative spring-boot:build-image -DskipTests
docker tag docker.io/library/query-node-native:0.24.0-SNAPSHOT 801563263500.dkr.ecr.eu-west-2.amazonaws.com/orbital:latest
docker push 801563263500.dkr.ecr.eu-west-2.amazonaws.com/orbital:latest
```
