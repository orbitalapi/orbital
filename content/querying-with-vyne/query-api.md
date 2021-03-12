---
title: Rest API
description: Vyne offers two REST apis for querying data. Simple Query and VyneQL.
---

All the examples here are based on the [Sample Instrument Service]().

## Simple Query API

### Querying for a list of instruments

Sample request to get all instruments from instrument-service:

```text
POST http://localhost:9022/api/query

Body:
{
    "expression":["demo.Instrument[]"],
    "facts":[],
    "queryMode":"DISCOVER",
    "resultMode":"SIMPLE"
}
```

Response:

```text
{
    "results": {
        "lang.taxi.Array<demo.Instrument>": [
            {
                "countryCode": "USA",
                "isin": "US0378331005",
                "name": "Apple",
                "symbol": "AAPL"
            },
            {
                "countryCode": "USA",
                "isin": "US02079K1079",
                "name": "Alphabet C (ex Google)",
                "symbol": "GOOG"
            },
            {
                "countryCode": "UK",
                "isin": "GB00B03MLX29",
                "name": "Royal Dutch Shell A",
                "symbol": "RDSA"
            }
        ]
    },
    "unmatchedNodes": [],
    "queryResponseId": "876e59fc-e36d-48af-96cd-80483edc3e61",
    "resultMode": "SIMPLE",
    "truncated": false,
    "duration": 11,
    "remoteCalls": [
        // removed for clarity
    ],
    "timings": {
        "REMOTE_CALL": 11,
        "ROOT": 1
    },
    "vyneCost": 0,
    "fullyResolved": true
}
```

Here is an equivalent query done via Vyne UI:

![](/assets/image%20%2838%29.png)

### Querying for a single instrument

Request:

```text
POST http://localhost:9022/api/query

Body:
{
    "expression":["demo.Instrument"],
    "facts":[{"typeName":"demo.instrument.Isin","value":"US0378331005"}],
    "queryMode":"DISCOVER",
    "resultMode":"SIMPLE"
}
```

Response:

```text
{
    "results": {
        "demo.Instrument": {
            "isin": "US0378331005",
            "name": "Apple",
            "symbol": "AAPL",
            "countryCode": "USA"
        }
    },
    "unmatchedNodes": [],
    "queryResponseId": "a33d5db5-eb73-491c-94c7-14f0102653bc",
    "resultMode": "SIMPLE",
    "truncated": false,
    "duration": 11,
    "remoteCalls": [
        // removed for clarity
    ],
    "timings": {
        "REMOTE_CALL": 9,
        "ROOT": 3
    },
    "vyneCost": 0,
    "fullyResolved": true
}
```

Here is an equivalent query done via Vyne UI:

![](/assets/image%20%282%29.png)

## VyneQL API

VyneQL offers simple, yet powerful way of expressing data queries, constraints and projections.

Building blocks of VyneQL:

* **given** - block allows to specify what is known to us \(facts\) e.g. ISIN number of an instrument that we are looking for.
* **findAll** - block defines what we querying for, e.g. an individual Instrument or a list of Instruments.
* **as** - projection block, defines type to which response should be transformed to. 

Sample query:

```text
given {
   isin: demo.instrument.Isin = "US0378331005"
}
findAll {
   demo.Instrument
}
as CommonInstrument
```

### Query for a list of instruments

In this example we going to ask Vyne to find all the Instruments.

Sample request to get all instruments from instrument-service

```text
POST http://localhost:9022/api/vyneql

Request body:
findAll {
   demo.Instrument[]
}
```

Response:

```text
{
    "results": {
        "lang.taxi.Array<demo.Instrument>": [
            {
                "countryCode": "USA",
                "isin": "US0378331005",
                "name": "Apple",
                "symbol": "AAPL"
            },
            {
                "countryCode": "USA",
                "isin": "US02079K1079",
                "name": "Alphabet C (ex Google)",
                "symbol": "GOOG"
            },
            {
                "countryCode": "UK",
                "isin": "GB00B03MLX29",
                "name": "Royal Dutch Shell A",
                "symbol": "RDSA"
            }
        ]
    },
    "unmatchedNodes": [],
    "queryResponseId": "3f52cfcc-059c-4206-88e0-51f36245bc97",
    "resultMode": "SIMPLE",
    "truncated": false,
    "duration": 9,
    "remoteCalls": [
       // removed for clarity
    ],
    "timings": {
        "REMOTE_CALL": 9,
        "ROOT": 1
    },
    "vyneCost": 0,
    "fullyResolved": true
}
```

### Querying for a single instrument

Request:

```text
given {
   isin: demo.instrument.Isin = "US0378331005"
}
findAll {
   demo.Instrument
}

```

Response:

```text
{
    "results": {
        "demo.Instrument": [
            {
                "isin": "US0378331005",
                "name": "Apple",
                "symbol": "AAPL",
                "countryCode": "USA"
            }
        ]
    },
    "unmatchedNodes": [],
    "queryResponseId": "487ac419-3823-47c0-b14f-7aa54a234575",
    "resultMode": "SIMPLE",
    "truncated": false,
    "duration": 20,
    "remoteCalls": [
        // removed for clarity
    ],
    "timings": {
        "REMOTE_CALL": 15,
        "ROOT": 6
    },
    "vyneCost": 0,
    "fullyResolved": true
}
```

### Querying with projection

The example below shows how to query for Orders \(created withing a given date range\) and then project results to common Order object.

```text
findAll {
   Order[] ( OrderDateTime  >= "2010-03-27T11:01:09", OrderDateTime < "2030-03-27T11:01:11" )
} as common.Order[]
```

### Programmatic API

...
