---
title: Publishing & sharing schemas
description: Learn how Vyne builds its schema from available services
---

## Overview

Vyne works by building up a federated schema of the services available, the data they offer, and the inputs they require.

In order to construct this view, services need to publish a schema describing their capabilities.  There's a few different approaches available for publication, which we'll explore here.

Schemas are described in [Taxi](https://docs.taxilang.org), a strongly typed semantic schema language.  However Vyne also supports consuming Swagger schemas, and enriching them with additional type metadata in Taxi.  We cover that in more detail here. \[TODO\].



## How schemas are shared - Centralised vs Distributed

### Centralised

When running centralised, services publish metadata that advertises they are making a schema available at a specific URL.  The service advertises details such as a unqiue name for the schema, a version \(following [SemVer](https://semver.org/)\), and a url that can be called to fetch the schema.

 This data is published to a centralised service discovery component, typically on startup.

Vyne periodically polls the service discovery service to find new and updated schemas.  When these are found, Vyne calls the url provided by the microservice to fetch it's schema.

![Vyne fetching schemas in a Centralised configuration](/assets/documentation-images-2-.png)

For quick-start and local developer experiences, Vyne can also run as a service discovery component, and ships with an instance of [Netflix Eureka](https://github.com/Netflix/eureka) embedded which can be optionally enabled.  This simplifies the components required for a quick start, but Vyne's embedded Eureka is not intended for production use.

#### What gets published?

* The name of the schema \(which should be unique for each service\)
* The SemVer version of the schema
* A URL to call to download the schema.

Note that the schema itself is not published, as these can become quite large.

#### Why don't services simply publish their schema's directly to Vyne?

When running in an distributed environment, services must be resilient to restarts and failure.  If services published their schemas directly to Vyne, it creates race conditions and ordering dependencies around the sequence that services start.  Instead, Vyne chooses to offload this to a centralised service in the infrastructure layer, which is expected to be "always on" - in this case, the service discovery layer.

This way, services and Vyne are free to re\(start\) in any order, and simply converge their state using the Service Discovery container.

#### Benefits of centralised schema discovery

* Simplier network configuration, and uses simple HTTP\(s\) traffic to share schema data
* Works well in containerised environments with virtual network layers

#### Drawbacks of centralised schema discovery

* Slower change detection, as updates are found by polling
* Service discovery container must support embedding additional metadata, otherwise requires an additional runtime component to aggregate schemas

### Distributed

> Currently supported on the JVM only, though support for JS and .NET is planned.  Get in touch if this is an important feature for you.

When configured to run in distributed mode, the services form a shared cluster of schema metadata - removing the need for a centralised schema service.

In this scenario, services discover each other using Multicast, and form a cluster.

This has the benefit of much faster restart times, as schemas are served and updated as soon as the service comes online.   

In addition to running as a distributed schema store, this model can optionally support running Vyne entirely embedded within services, removing the need for a centralised Vyne service entirely.  This is discussed in more detail in [Deployment Configurations](deployment-configurations.md).

![](/assets/documentation-images-3-.png)



#### Benefits of centralised schema discovery

* Updates to schemas are instant
* Reduces dependency on centralised infrastructure

#### Drawbacks of centralised schema discovery

* Requires the network layer to support Multicast.  Of note, this is not supported by default in orchestrated containerised environments \(such as Docker Swarm and Kubernetes\).

## Generating schemas from code

> Currently supported in Spring Boot only

![](https://img.shields.io/badge/dynamic/xml.svg?label=Latest&url=http%3A%2F%2Frepo.vyne.co%2Frelease%2Fio%2Fvyne%2Fplatform%2Fmaven-metadata.xml&query=%2F%2Frelease&colorB=green&prefix=v&style=for-the-badge&logo=kotlin&logoColor=white)

Vyne has great support for Spring Boot, to allow us to generate Taxi schemas directly from our code, which is by far the easiest way to get started.

In a fashion similar to SpringFox's Swagger support, getting started is as simple as annotating our classes and domain models.



This is covered in more detail in our guides.



## Using hand-crafted schemas within your application

## Publishing schemas from a git repository

