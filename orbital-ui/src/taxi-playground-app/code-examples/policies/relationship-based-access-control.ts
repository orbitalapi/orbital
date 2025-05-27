import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const RelationshipBasedAccessControl: StubQueryMessageWithSlug = {
  "title": "Relationship based access control (ReBAC)",
  "slug": "policy-rebac",
  "query": {
    "schema": `import taxi.stdlib.concat

// This example demonstrates data filtering based on cross-service authorization rules.
// Filters sensitive trade data by comparing trading entity against a "sensitive" list,
// with different visibility rules based on user roles.

// Data Sources:
// 1. Kafka stream of trade events
// 2. Companies whose trade data is sensitive, loaded from a separate API
// 3. User role information from auth layer

// Main Stream Definition
// Typically defined in Avro/Protobuf, simplified here for demonstration
// Note, we've configured a stub over in the stubs panel
// to return some data here.
service StockTradeApi {
   stream trades:Stream<StockTradeEvent>
}

// Stock trades, emitted on a Kafka topic
model StockTradeEvent {
  symbol : Symbol inherits String // Stock symbol
  price: Price inherits Decimal // Trade price
  tradedQuantity : TradedQuantity inherits Int // Volume of trade
  tradingEntity : CompanyName inherits String  // Company executing trade
}

// List of companies who trading activies should be treated
// as sensitive, emitted from a REST API
closed model SensitiveCompanyList {
  companies: CompanyName[]
}

// REST API
// Note, we've configured a stub over in the stubs panel
// to return some data here.
service SensitiveCompaniesApi {
  operation getSensitiveCompanies():SensitiveCompanyList
}

// Authentication Models
type UserGroup inherits String
model User {
  roles:UserGroup[]  // User's assigned roles for authorization
}

// Access Control Policy
// Implements three-tier access:
// 1. Managers: Full access to all trade data
// 2. Restricted company trades: No access (filtered out)
// 3. Standard users: Limited access with hidden customer and quantity
policy FilterCustomerFromTrade against StockTradeEvent (
    trade: StockTradeEvent,
    user: User,
    sensitiveCompanies: SensitiveCompanyList
) -> {
  read {
    when {
      // TIER 1: Manager access - full visibility
      user.roles.contains('MANAGER') -> StockTradeEvent

      // TIER 2: Restricted company filter
      // Removes entire trade record if trading entity is classified
      sensitiveCompanies.companies.contains(trade.tradingEntity) -> null

      // TIER 3: Standard access
      // Shows trade but hides sensitive fields
      else -> StockTradeEvent as { ... except { tradingEntity, tradedQuantity } }
    }
  }
}`,
    "stubs": [
      {
        "operationName": "trades",
        "response": "[\n  {\n    \"symbol\": \"NFLX\",\n    \"price\": 645.28,\n    \"tradedQuantity\": 120,\n    \"tradingEntity\": \"Alpha Investments\"\n  },\n  {\n    \"symbol\": \"NVDA\",\n    \"price\": 324.50,\n    \"tradedQuantity\": 85,\n    \"tradingEntity\": \"Beta Capital\"\n  },\n  {\n    \"symbol\": \"BABA\",\n    \"price\": 175.60,\n    \"tradedQuantity\": 200,\n    \"tradingEntity\": \"Gamma Holdings\"\n  },\n  {\n    \"symbol\": \"ORCL\",\n    \"price\": 88.77,\n    \"tradedQuantity\": 300,\n    \"tradingEntity\": \"Delta Traders\"\n  },\n  {\n    \"symbol\": \"IBM\",\n    \"price\": 142.93,\n    \"tradedQuantity\": 60,\n    \"tradingEntity\": \"Omega Investments\"\n  }\n]\n"
      },
      {
        "operationName": "getSensitiveCompanies",
        "response": "{\n   \"companies\" : [\"Alpha Investments\" ,\"Gamma Holdings\" ]\n}"
      }
    ],
    "query": `// Try this query a few different ways.
// When the user has the role of MANAGER, then all 5 trades are emitted.
// Otherwise, 2 of the trading entities get filtered entirely,
// and the others have their sensitive fields redacted.
//
// After running this query, check the "Requests" panel in the results
// to see how the RestrictedCompaniesApi was invoked to load the
// list of restricted companies
given { user: User = { roles : ['MANAGER']}}
stream { StockTradeEvent }`,
    "parameters": {}
  }
}
