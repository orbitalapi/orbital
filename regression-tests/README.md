
# How to Run
* Run FileSchemaServer with -Dtaxi.schema-local-storage set to /taxonomy folder of this repository.
* Get the full stack app and running with preferably empty Cask Database
* Open a shell and goto 'scripts' folder of this repository
* ./loadstatic.sh
* ./load-broker-data.sh
* You should end up with 26 casks

run:

findAll {
	cacib.orders.Order[]
} as { cacib.imad.Order[] }


# How To Run System Tests


* Run:

mvn verify

Above will download 'eureka', 'vyne', 'file-schema-server' and 'Cask' docker images with 'latest' tag and run the system tests against them. These will run against the taxonomy in 'taxonomy' folder, 'static' data in 'vyne-static' folder and broker data in src/test/resources/ folder.


* If you'd like to run tests against a particular 'vyne docker' label, specify the label as:

mvn verify -Dvyne.tag=<DESIRED DOCKER IMAGE LABEL>

e.g.

mvn verify -Dvyne-tag=latest-snasphost

# I don't want to deal with docker containers, I just want to run the system (vyne, cask etc.) locally and run test against my local


* Make sure that your file-schema-server initialised to read the taxonomy from 'taxonomy' folder of this project.
* Make sure that you start Cask against an empty database.

* Once Eureka, FileSchemaServer, Query Service and Cask are up and running:
    * Open a shell and goto vyne folder of this repository
    * ./loadstatic.sh
    * goto src/test/resources
    * ./load-broker-data.sh
    
 and run:
    mvn verify -Drun.mode=local

Note: If you want to run the tests from 'IntelliJ' without dealing with maven against your local setup, goto TestHelper.kt line 44
and change:

     
     private var runMode: RunMode = RunMode.Docker
     
as:

    
    private var runMode: RunMode = RunMode.Local
    
