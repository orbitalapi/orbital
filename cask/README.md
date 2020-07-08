# Development Environment

## Prerequisites
* Java 8
* Maven 3.x
* Docker/Docker-compose

## How to run locally

### Postgres DB
Note: You must have docker/docker-compose installed locally.
 
```
cd vyne/cask

# to start postgres db
docker-compose up

# to stop postgres db
docker-compose down
```

### QueryServiceApp
* Select 'QueryServiceApp' in intellij
* Enable embedded-discovery profile ```-Dspring.profiles.active=embedded-discovery``` (VM argument)
* Right-click and run 

### FileSchemaServer
* Create schemas folder, e.g ```C:\dev\workspace\_schemas```
* Create test-schema.taxi file with the following content
```
type alias Price as Decimal
type alias Symbol as String
type OrderWindowSummary {
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    // Added column
    high : Price by xpath("/High")
    // Changed column
    close : Price by xpath("/Close")
}
```
* Select 'QueryServiceApp' in intellij
* Set schema directory e.g. ```-Dtaxi.project-home=C:\dev\workspace\_schemas``` (VM argument)
* Right-click and run 

### Cask
* Select 'CaskApp' in intellij
* Enable local spring profile ```-Dspring.profiles.active=local``` (VM argument)
* Right-click and run 
* Open http://localhost:8800/static/index.html testharness page 

### Cask Service Operation Generation

see [How cask generates service operations?](cask_operations.md)
