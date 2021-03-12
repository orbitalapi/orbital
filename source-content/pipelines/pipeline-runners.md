---
description: >-
  The pipelines Runner is a container to run Pipelines. It will initiate the
  connections to the datasources, ingest the data, optionally transform it, and
  write it to the output datasource.
---

# Pipeline Runners

## Prerequisites

* Running Eureka server
* Running Vyne query server
* Running Pipelines Orchestrator
* Taxi schema for ingested data

## How does it work

The pipeline Runner receives pipeline descriptions through a REST API. On reception, it is going to instantiate the two input and output Transports. Those transports will be responsible to initiate and maintain connections and communication to the datasources

Whilst the runners expose a REST API to schedule pipelines, it is recommended to use the Pipelines Orchestrator API to submit and manage your pipelines.

## Reporting to Orchestrator

As mentioned in the Pipelines Orchestrator documentation, the Pipeline Runners write their current state into their Eureka instance metadata.  
  
On top of that, the Pipeline Runners also communicate back events to the Pipelines Orchestator through the Event API. Currently, only the logs of the runners are reported back to the Orchestrator and no action except of logging is performed on reception of those events. Future versions will be more elaborated.



