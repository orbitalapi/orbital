import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const RoleBasedKafka: StubQueryMessageWithSlug = {
  "title": "Role based auth on Kafka",
  "slug": "kafka-role-based-auth",
  "query": {
    "schema": `import taxi.stdlib.concat
// This example shows applying field-based access controls
// on a Kafka stream of trade events.


// A kafka stream, emitting stock trade events.
// Normally, this would be defined in an Avro / Protobuf schema,
// but we've defined it here in Taxi to keep the sample short.
//
// Note, we've configured a stub over in the stubs panel
// to return some data here.
service StockTradeApi {
   stream trades:Stream<StockTradeEvent>
}

// The event
model StockTradeEvent {
  symbol : Symbol inherits String
  price: Price inherits Decimal
  tradedQuantity : TradedQuantity inherits Int
  customer : CustomerName inherits String
}

// This is the User data, typically provided by your auth layer
type UserGroup inherits String
model User {
  roles:UserGroup[]
}

// A policy to secure it
policy FilterCustomerFromTrade against StockTradeEvent (user: User) -> {
  read {
    when {
      // Managers see everything
      user.roles.contains('MANAGER') -> StockTradeEvent
      // Everyone else has customer and quantity hidden
      else -> StockTradeEvent as {
        customer: CustomerName = null
        tradedQuantity: TradedQuantity = null
        ...
      }
    }
  }
}
`,
    "stubs": [
      {
        "operationName": "trades",
        "response": "[\n  {\n    \"symbol\": \"AAPL\",\n    \"price\": 174.56,\n    \"tradedQuantity\": 100,\n    \"customer\": \"John Doe\"\n  },\n  {\n    \"symbol\": \"GOOGL\",\n    \"price\": 2823.15,\n    \"tradedQuantity\": 50,\n    \"customer\": \"Jane Smith\"\n  },\n  {\n    \"symbol\": \"TSLA\",\n    \"price\": 759.29,\n    \"tradedQuantity\": 200,\n    \"customer\": \"David Lee\"\n  },\n  {\n    \"symbol\": \"MSFT\",\n    \"price\": 299.72,\n    \"tradedQuantity\": 150,\n    \"customer\": \"Emily Johnson\"\n  },\n  {\n    \"symbol\": \"AMZN\",\n    \"price\": 3459.12,\n    \"tradedQuantity\": 75,\n    \"customer\": \"Chris Evans\"\n  }\n]\n"
      }
    ],
    "query": `// Now stream it securely
// Note: The user info here is normally provided by
// your authentication layer
// But for now, we'll hard-code it.
// Try changing the role to something other than MANAGER
// and re-running the query
given { user: User = { roles : ['MANAGER']}}
stream { StockTradeEvent }
`,
    "parameters": {}
  }
}
