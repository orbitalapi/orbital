import {StubQueryMessageWithSlug} from "../../../app/services/query.service";


// Example 3: Data Enrichment
export const EnrichingCsvData : StubQueryMessageWithSlug = {
  "title": "Enriching CSV Data",
  "slug": "csv-enrichment",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": false,
      "showReadme": true,
      "showQuery": true
    },
    "schema": `import com.orbitalhq.formats.Csv

type ProductName inherits String
type Price inherits Double
type StockLevel inherits Int
type CountryCode inherits String
type CountryName inherits String

@Csv
closed model InventoryReport {
    ProductTitle : ProductName
    Stock_Level : StockLevel
    UnitCost : Price
    Country_Origin_Code : CountryCode
}

// Country lookup service
closed model CountryLookupResponse {
    name : CountryName
    code : CountryCode
}

service CountryService {
    operation getCountry(CountryCode):CountryLookupResponse
}

service WarehouseData {
    operation getInventory():InventoryReport[]
}`,
    "query": `given { CountryCode = 'DE' }
// CountryName is an attribute on the
// CountryLookupResponse. We could've asked for the entire object is we wanted.
find { CountryLookupResponse }
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getInventory",
        "response": "ProductTitle,Stock_Level,UnitCost,Country_Origin_Code\nSmartphone,12,437.84,DE\nRunning Shoes,56,34.62,CN\nBluetooth Speaker,54,252.12,US"
      },
      {
        "operationName": "getCountry",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "p0",
                "value": "DE"
              }
            ],
            "response": {
              "body": "{ \"name\": \"Germany\", \"code\": \"DE\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": "CN"
              }
            ],
            "response": {
              "body": "{ \"name\": \"China\", \"code\": \"CN\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": "US"
              }
            ],
            "response": {
              "body": "{ \"name\": \"United States\", \"code\": \"US\" }"
            }
          }
        ]
      }
    ],
    "readme": "## Enriching CSV Data\nThis example demonstrates how to enrich CSV data using an external API.\n\n### The Scenario\nOur inventory data contains country codes, but we want to display full country names.\n\n### View the Raw Data\nFirst, let's see our inventory with country codes:\n\n```taxiql\nfind { InventoryReport[] }\n```\n\n### Testing the Country Service\nWe can look up individual country names:\n\n```taxiql\ngiven { CountryCode = 'DE' }\n// CountryName is an attribute on the \n// CountryLookupResponse. We could've asked for the entire object is we wanted.\nfind { CountryName } \n```\n\nHere's how all the data and services hang together:\n\n```components \n{\n   \"showTypeToolbar\" : false,\n   \"members\" : {\n      \"InventoryReport\" : {},\n      \"CountryLookupResponse\" : {},\n      \"CountryService\" : {}\n   }\n}\n```\n\n### Enriching All Records\nNow let's enrich all our inventory data with country names:\n\n```taxiql\nfind {\n    inventory: InventoryReport[] as {\n        product: ProductName\n        price: Price\n        stock: StockLevel\n        countryName: CountryName\n    }[]\n}\n```\n\n> [!TIP] \n> After running this query, be sure to check the \n> lineage diagram and requests panel, to see\n> what happened\n\nTaxiQL automatically:\n1. Extracts the country code\n2. Calls the country service\n3. Incorporates the response\n\nCheck the Requests panel to see the API calls being made!"
  }
}
