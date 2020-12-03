---
description: >-
  Casks are a place to store file based data, and make it queryable
  semantically.
---

# Overview

> #### Prerequisites:
>
> * Running Vyne query server
> * A schema server

Not all data is housed behind a microservice or API. Sometimes, you receive data that you simply want to store and query later.  It could be an end-of-day feed, a stream of data from a 3rd party, or even static reference data.

Casks provide a way of ingesting data in a variety of different formats, applying a Taxi schema, and making it available to query semantically.  Cask services also handle the generation of RESTful API's, and publishing the schema up to Vyne, meaning the data becomes discoverable in Vyne queries.

Data can be ingested in any of the following formats:

* CSV
* JSON
* XML



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

All the Cask data can be accessed via [Query Api](../querying-with-vyne/query-api.md).



