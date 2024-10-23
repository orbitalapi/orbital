import {StubQueryMessageWithSlug} from "../../app/services/query.service";

export const KafkaApiWebsocket: StubQueryMessageWithSlug = {
  "title": "Enrich Kafka and publish over websocket",
  "slug": "kafka-api-websocket",
  "query": {
    "schema": `// This demo shows:
//
// * Pulling data from a stream (such as a Kafka topic)
// * Enriching it with company details from a REST API (company name and HQ)
// * And exposing that data over a Websocket
//
// Try running the query in the query panel, and see the data
// get enriched.
//
// Note: As this is a playground, all server interactions are
// stubbed out, and publishing over a websocket
// isn't provided.

closed model StockQuote {
  symbol: Ticker inherits String
  timestamp: StockPriceTimestamp inherits Instant
  price : StockPrice inherits Decimal
}

service StockPriceService {
  stream stockPrices:Stream<StockQuote>
}

closed model Stock {
  name : CompanyName inherits String
  headquarters: CityName inherits String
}
service TickerService {
  operation getStock(ticker:Ticker):Stock
}`,
    "query": `import taxi.http.WebsocketOperation
import CityName
import CompanyName
import StockPrice
import Ticker
import StockQuote

@WebsocketOperation(path = "/api/ws/stockQuotes")
query EnrichedStockQuote {
    stream { StockQuote } as {
        // Ticker and stock price come from the Kafka feed
        ticker: Ticker
        price: StockPrice
        // Company name and HQ city come from a REST API
        name : CompanyName
        hq: CityName
    }[]
}
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "stockPrices",
        "response": "[\n  {\n    \"symbol\": \"AAPL\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 174.45\n  },\n  {\n    \"symbol\": \"GOOGL\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 2743.67\n  },\n  {\n    \"symbol\": \"MSFT\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 322.31\n  },\n  {\n    \"symbol\": \"AMZN\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 3345.78\n  },\n  {\n    \"symbol\": \"TSLA\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 850.12\n  },\n  {\n    \"symbol\": \"FB\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 336.47\n  },\n  {\n    \"symbol\": \"NFLX\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 590.44\n  },\n  {\n    \"symbol\": \"NVDA\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 210.25\n  },\n  {\n    \"symbol\": \"BABA\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 145.67\n  },\n  {\n    \"symbol\": \"ORCL\",\n    \"timestamp\": \"2024-10-23T10:15:30Z\",\n    \"price\": 95.63\n  }\n]\n"
      },
      {
        "operationName": "getStock",
        "response": "{\n   \"name\" : \"Oracle\",\n   \"headquarters\":\"Redwood City, California, USA\"\n}",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "ORCL"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Oracle Corporation\",\n   \"headquarters\":\"Redwood City, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "AAPL"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Apple Inc.\",\n   \"headquarters\":\"Cupertino, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "GOOGL"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Alphabet Inc.\",\n   \"headquarters\":\"Mountain View, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "MSFT"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Microsoft Corporation\",\n   \"headquarters\":\"Redmond, Washington, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "AMZN"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Amazon.com, Inc.\",\n   \"headquarters\":\"Seattle, Washington, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "TSLA"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Tesla, Inc.\",\n   \"headquarters\":\"Palo Alto, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "FB"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Meta Platforms, Inc.\",\n   \"headquarters\":\"Menlo Park, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "NFLX"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Netflix, Inc.\",\n   \"headquarters\":\"Los Gatos, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "NVDA"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"NVIDIA Corporation\",\n   \"headquarters\":\"Santa Clara, California, USA\"\n}"
            }
          },
          {
            "inputs": [
              {
                "name": "ticker",
                "value": "BABA"
              }
            ],
            "response": {
              "body": "{\n   \"name\" : \"Alibaba Group Holding Limited\",\n   \"headquarters\":\"Hangzhou, Zhejiang, China\"\n}"
            }
          }
        ]
      }
    ],
    "readme": ""
  }
}
