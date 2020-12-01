---
description: >-
  Cask server is as service that ingests data in various input formats, projects
  them to a taxi type (e.g. Trade or Order) and exposes automatically queryable
  API in Vyne.
---

# Cask Server

## Prerequisites

* Running Eureka server
* Running Vyne query server
* Taxi schema for ingested data

## Benefits of using Cask

Casks provide a simple way of storing data and making it queryable using semantic discovery

Casks ingest your data \(e.g. list of Orders\), add semantic description \(e.g. Taxi Order type\), store it and automatically expose set of operations via Vyne query API. Among many operations the newly created api will support:

* querying by all type fields \(e.g. findByOrderId\),
* querying by date ranges e.g. findOrdersBetween\(start:OrderDate, end:OrderDate\).

## Example Cask Schema

Let's say we got the following [Taxi ](https://docs.taxilang.org/taxi-language)type:

```text
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

Note: **@Between** extension, it's telling Cask to expose queryable endpoint accepting start and end dates \(date range query\).

Once the first CSV data is loaded to Cask, it will automatically expose the following services to Vyne:

```text
@ServiceDiscoveryClient(serviceName = "cask")
service OrderCaskService {
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/id/{vyne.casks.OrderId}")
   operation findById( @PathVariable(name = "id") id : vyne.casks.OrderId ) : vyne.casks.Order[]
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/symbol/{vyne.casks.Symbol}")
   operation findBySymbol( @PathVariable(name = "symbol") symbol : vyne.casks.Symbol ) : vyne.casks.Order[]
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/price/{vyne.casks.Price}")
   operation findByPrice( @PathVariable(name = "price") price : vyne.casks.Price ) : vyne.casks.Order[]
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/side/{vyne.casks.Side}")
   operation findBySide( @PathVariable(name = "side") side : vyne.casks.Side ) : vyne.casks.Order[]
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/date/{vyne.casks.OrderDate}")
   operation findByDate( @PathVariable(name = "date") date : vyne.casks.OrderDate ) : vyne.casks.Order[]
   @HttpOperation(method = "GET" , url = "/api/cask/vyne/casks/Order/date/Between/{start}/{end}")
   operation findByDateBetween( @PathVariable(name = "start") start : vyne.casks.OrderDate, @PathVariable(name = "end") end : vyne.casks.OrderDate ) : vyne.casks.Order[]( vyne.casks.OrderDate >= start, vyne.casks.OrderDate < end )
}

```

All the Cask data can be accessed via [Query Api](../running-a-local-taxonomy-editor-environment/query-api.md).



