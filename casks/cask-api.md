---
description: >-
  Cask API allows for an easy way of ingesting your data and exposing it as a
  query-able API in Vyne.
---

# Cask Ingestion API

Cask offers two APIs:

* Websocket API
* Rest API

In both cases, a cask is created if not already present when data is submitted.

## Websocket API

Websockets are used for publishing a continuous stream of data to a cask.

Once a Websocket session is established, the client can submit many ingestion requests. The body of the ingestion request is just a text. 

The format of the URL is as follows:

`ws://localhost:8800/cask/[contentType]/[typeReference]?debug=true&[otherParams]`

**contentType**:

* csv
* json

**typeReference**:

* Fully qualified name of the taxi type that will be used to ingest data \(e.g.`com.acme.BlogPost`\)

 **General parameters:**

* debug - enables ingester response \(default debug=false\)
* nullValue - any number of parameter & separated. e.g. nullValue=EMPTY will result in ingester not mapping those values

**otherParams**:

* csvDelimiter - csv file column separator \(default value ","\)
* csvFirstRecordAsHeader - treat first csv row as list of column names \(default value true\)

### URI examples

#### **JSON**

`ws://cask:8800/cask/json/com.acme.BlogPost`

`ws://cask:8800/cask/json/com.acme.BlogPost?debug=true`

#### **CSV**

`ws://cask:8800/cask/csv/com.acme.BlogPost?debug=true&csvDelimiter=,`

`ws://cask:8800/cask/csv/com.acme.BlogPost?csvDelimiter=;&nullValue=NULL&nullValue=N/A`

`ws://cask:8800/cask/csv/com.acme.BlogPost?csvDelimiter=:&csvFirstRecordAsHeader=false`

### Payload examples

#### JSON

```text
[
  {
    "id": "order1",
    "symbol": "BTCUSD",
    "price": "10000",
    "side": "SELL",
    "date": "2020-01-01T12:00:00.000Z"
  },
  {
    "id": "order2",
    "symbol": "ETHUSD",
    "price": "300",
    "side": "BUY",
    "date": "2020-01-02T12:00:00.000Z"
  }
]
```

#### CSV

```text
id,symbol,price,side,date
order1,BTCUSD,10001,SELL,2020-01-01T12:00:00.000Z
order2,ETHUSD,301,SELL,2020-01-02T12:00:00.000Z
```

## Rest API

Rest API is designed to offer fast and easy way to upload data to Cask. The API is identical to Websocket API \(the only difference is /api in the url\).

`POST http://cask:8800/api/ingest/[contentType]/[typeReference]?debug=true&[otherParams]`

### Examples

**CSV**

```text
POST http://localhost:8800/api/ingest/csv/vyne.casks.Order?debug=true&csvDelimiter=,

Payload:
id,symbol,price,side,date
order1,BTCUSD,10001,SELL,2020-01-01T12:00:00.000Z
order2,ETHUSD,301,SELL,2020-01-02T12:00:00.000Z
```

**JSON**

```text
POST http://localhost:8800/api/ingest/json/vyne.casks.Order?debug=true

Payload:
{
    "id": "order1",
    "symbol": "BTCUSD",
    "price": "10000",
    "side": "SELL",
    "date": "2020-02-01T12:00:00.000Z"
}
```

