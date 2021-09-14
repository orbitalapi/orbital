---
title: Pipelines Orchestrator
description: >-
  The pipelines Orchestrator allows you to schedule pipelines to the runners
  through a REST API and visualise them.
---

## Overview

## Prerequisites

...

* Running Eureka server
* Running Vyne query server
* Running one or many Pipeline Runner
* Taxi schema for ingested data

## Pipeline Orchestrator API

## How does it work

...

The Pipeline Orchestrator can be access by a REST API or through a basic UI at `http://pipelines-orchestrator:9600/index.html`

## Pipeline Orchestrator UI

When submitting a pipeline, the Orchestrator will pick an available runner using load-balancing and send the pipeline to it. Once the Runner started the pipeline, it will publish its status into its Eureka instance metadata. The Orchestator periodically polls Eureka to gather instances metadata and updates the information it has about all the runners.

...

## Recovery



As the Runners communicates their state through Eureka metadata, there is no need to have the Orchestrator running in order to have a Runner functionning properly. At starting, the Orchestrator is going fetch all the information he needs from Eureka to rebuild its state.  
  
Conversely, there is no need to have a Runner running in order to have the Orchestrator running properly. When a runner is shutdown for any reason, the Orchestrator is going to pick up this event and assign the pipeline to another available Runner.  
  
If no available Runners are available, the Orchestrator is going to keep the pending Pipelines in memory and wait until a Runner is running 
