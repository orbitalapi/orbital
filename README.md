# Development Environment

## Server side
* JDK8 (todo upgrade to Java 11)
* Maven 3.x

## Client Side

* Node.js 12.16.3
* Npm (tested with version 6.9.0)

# Running App Locally for development purpose

## Server

* Run 'QueryServiceApp' with embedded-discovery and local  spring profiles
* embedded-discovery provides and embedded Eureka server so that client app can register against it.
* local profile enables REST request coming from localhost:4200 by modifying the CORS settings accordingly. This profile
requires only if you're planning run client app on localhost:4200 (see below). If that is not the case, build the client app by running
'mvn clean install -DskipTests' or by running 'npm build' on vyne-query-service/src/main/web 

## Client

* On a terminal / cmd shell, goto vyne-query-service/src/main/web  and run 'npm run start-dev'. That will fire up and angular development server
running on localhost:4200

* If you are not interested in client side development, you can simply run 'mvn clean install -DskipTests' for 'vyne-query-service' and
run QueryServiceApp. That'd make client application avalilable at localhost:9022

# Running application locally using docker

## Windows

### How to install docker
Follow instructions here:
* https://docs.docker.com/docker-for-windows/install/
This should install docker engine and docker-compose.

### How to start docker
* Click windows start button, search/type for Docker, and select Docker Desktop in the search results.
* Wait for it to start, about 30-60 seconds

## How to building vyne docker containers locally
Command below should compile code (java/npm) and build docker containers locally:
```
mvn clean install docker:build -Dmaven.test.skip=true -P snapshot-release
```
Note: vyne-query-service docker images is huge, on windows the build may look like it hanged, please be patient.

## How starting vyne

Open cmd console or git-bash console in vyne root project (e.g. ```C:\dev\workspace\vyne``` and type in:
```
docker-compse up
```
this should pull down all the docker containers fr

## How to stopping vyne
* ctrl+c - this should stop gracefully all the containers, if not
* if not use the command below:
```
docker-compose down
```

## How to accessing vyne UI
* http://localhost:9022

# Unix/Ubuntu
...