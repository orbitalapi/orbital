---
title: UI
description: Learn what you can do with Vyne's powerful UI
---

Vyne comes with powerful UI that allows you to view and query the state of the platform.

## Type explorer

It allows you to view and search for all deployed Types and Services.

![](/assets/image%20%2829%29.png)

## Schema Explorer

It allows you to view full definition \(source\) and version of all your Types and Services. 

![](/assets/image%20%2820%29.png)

## Query builder

Extremely powerful tool when developing new services or testing existing deployment. It gives full view of how the query was executed and what services have been called in order to return the response. It also provides useful timing information handy when troubleshooting slow services.

![](/assets/image%20%2822%29.png)

## Data Explorer

This tool is designed to help everyone who designs Schemas, in quickly previewing how Vyne would extract data from JSON or CSV files and map it to a Taxi type.

In our example we going to use the following Order type deployed to Vyne:

```text
namespace demo

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

Once the Order type is defined in vyne we can drag crypto-orders.csv 

```text
id,symbol,price,side,date
order1,BTCUSD,10001,SELL,2020-01-01T12:00:00.000Z
order2,ETHUSD,301,SELL,2020-01-02T12:00:00.000Z
order3,BTCUSD,10000,BUY,2020-01-03T12:00:00.000Z
order4,ETHUSD,300,BUY,2020-01-04T12:00:00.000Z
```

and drop it in the data explorer:

![](/assets/image%20%2834%29.png)

After selecting target type demo.Order you should be able to see parsed data:

![](/assets/image%20%284%29.png)

## Query History

Vyne allows for recording queries executed by the engine. This mode is handy in dev/test environment where you can see what queries been executed and preview results and performance of all the calls.

![](/assets/image%20%283%29.png)
