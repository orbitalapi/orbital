---
title: Cask Examples
description: Examples of how to run cask
---

import { Link } from "gatsby"

To run cask you need a running instance of Vyne server:

## Prerequisites

* Eureka Server \(for components discovery\)
* Vyne Query Server \(the heart of the system\)
* File Schema Server \(taxi schema store\)
* Cask Server Server \(read cache\)

By far the quickest way is to use docker compose to bootstrap all those components. 

If you don't have docker/docker-compose installed on your machine, follow these instructions: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)

## Starting cask with docker-compose 

Save the script below in your target location e.g. **/vyne/demos/**docker-compose.yml

This script can be found in our git repository: [https://gitlab.com/vyne/vyne-taxonomy-environment](https://gitlab.com/vyne/vyne-taxonomy-environment)

```text
version: "3.3"
services:
   vyne:
      image: vyneco/vyne:latest-snapshot
      ports:
         - 5701-5705
         - 9022:9022
      environment:
         PROFILE: embedded-discovery,inmemory-query-history

   file-schema-server:
      image: vyneco/file-schema-server:latest-snapshot
      ports:
         - 5701-5705
      volumes:
         - ./schemas:/var/lib/vyne/schemas
      environment:
         OPTIONS: \
            --taxi.schema-local-storage=/var/lib/vyne/schemas \
            --eureka.uri=http://vyne:9022

   cask:
      image: vyneco/cask:latest-snapshot
      depends_on:
         - vyne
      environment:
         PROFILE: local
         OPTIONS: --eureka.uri=http://vyne:9022
      ports:
         - 5701-5705
         - 15432:5432
         - 8800:8800

```

To start:

> docker-compose up

To stop:

> ctrl+c or docker-compose down

To clear the state of volumes and containers run this command:

> docker volume prune  
>
> docker container prune

To pull the latest version of containers:

> docker-compose pull

## Defining taxi schema

In our example we going to define schema for simple crypto order. The order has a lot more fields but in our example we use only the basic attributes to demonstrate the Cask concepts. Save this content to crypto-order.taxi and place in **/vyne/demos/schemas/**crypto-order.taxi

```text
namespace vyne.casks

type alias OrderId as String
type alias Symbol as String
type alias Price as Decimal
type alias Side as String
type alias OrderDate as Instant

type Order {
    id: OrderId by column(1)
    symbol : Symbol by column(2)
    price : Price by column(3)
    side: Side by column(4)
    @Between
    date: OrderDate by column(5)
}
```

## Preparing CSV Data

Data below represents couple of crypto orders in CSV format. All of the fields are mapped in the schema \(see above\). The values are extracted via column index \(1-based\).

```text
id,symbol,price,side,date
order1,BTCUSD,10001,SELL,2020-01-01T12:00:00.000Z
order2,ETHUSD,301,SELL,2020-01-02T12:00:00.000Z
order3,BTCUSD,10000,BUY,2020-01-03T12:00:00.000Z
order4,ETHUSD,300,BUY,2020-01-04T12:00:00.000Z
```

## Ingesting CSV Data

At the time of this writing Cask provides two endpoints for data ingestion.

* Websocket
* Rest

For the purpose of this demo we going to use only Rest endpoint as it's the simplest one to test. You can use Postman, Curl or other tools to send the data for ingestion.

**Request**

```text
POST http://localhost:8800/api/cask/csv/vyne.casks.Order?debug=true&csvDelimiter=,
ContentType: Text
Body:
id,symbol,price,side,date
order1,BTCUSD,10001,SELL,2020-01-01T12:00:00.000Z
order2,ETHUSD,301,SELL,2020-01-02T12:00:00.000Z
order3,BTCUSD,10000,BUY,2020-01-03T12:00:00.000Z
order4,ETHUSD,300,BUY,2020-01-04T12:00:00.000Z
```

**Response**

```text
{
    "result": "SUCCESS",
    "message": "Successfully ingested 4 records"
}
```

At this point Cask should store the data internally and expose number of operations for querying. 

To see the full list go to [http://localhost:9022/schema-explorer](http://localhost:9022/schema-explorer) and search for 'OrderCaskService'.

## Querying Data

{% page-ref page="../querying-with-vyne/query-api.md" %}

For this demo we going to use VyneQL.

**Request**

```text
POST http://localhost:9022/api/vyneql

Payload:
findAll {
   vyne.casks.Order[](
      OrderDate  >= "2020-03-01T00:00:00.000Z",
      OrderDate < "2020-01-04T12:00:00.000Z"
   )
} 


```

**Response**

```text
{
    "results": {
        "lang.taxi.Array<vyne.casks.Order>": [
            {
                "id": "order1",
                "symbol": "BTCUSD",
                "price": 10001.0,
                "side": "SELL",
                "date": "2020-01-01T12:00:00.000Z"
            },
            {
                "id": "order2",
                "symbol": "ETHUSD",
                "price": 301.0,
                "side": "SELL",
                "date": "2020-01-02T12:00:00.000Z"
            },
            {
                "id": "order3",
                "symbol": "BTCUSD",
                "price": 10000.0,
                "side": "BUY",
                "date": "2020-01-03T12:00:00.000Z"
            },
            {
                "id": "order4",
                "symbol": "ETHUSD",
                "price": 300.0,
                "side": "BUY",
                "date": "2020-01-04T12:00:00.000Z"
            }
        ]
    },
    "unmatchedNodes": [],
    "queryResponseId": "9b88b65b-0812-433f-9dea-d7963b705ca0",
    "resultMode": "SIMPLE",
    "truncated": false,
    "duration": 22,
    "remoteCalls": [
        // ... ommited for code clarity
    ],
    "timings": {
        "REMOTE_CALL": 19,
        "ROOT": 6
    },
    "vyneCost": 0,
    "fullyResolved": true
}
```

## Vyne Query History

To get information about what calls have been made go to [http://localhost:9022/query-history](http://localhost:9022/query-history) and select last request. The result shows what calls Vyne made to execute the query.

![](/assets/image%20%2815%29.png)

## Summary

In this section you have learned:

* What is Cask
* How to start Vyne/Cask stack
* How to prepare sample taxi schema
* How to ingest and query for data from Cask
* How to locate operations exposed by Cask
* How to view query history

