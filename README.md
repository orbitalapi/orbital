# Development Environment

## Client Side

Install

* npm (tested with version 6.9.0)

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
