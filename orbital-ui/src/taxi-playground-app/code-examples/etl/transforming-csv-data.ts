import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

/**
 * I've left this for reference, but this example
 * has been broken down into smaller examples
 *
 * @deprecated
 */
export const TransformingCsvData: StubQueryMessageWithSlug = {
  "title": "Enriching, aggregating and persisting CSV",
  "slug": "etl-aggregation-with-csv",
  "query": {
    "layout": {
      "showSchema": false,
      "showDiagram": false,
      "showReadme": true,
      "showQuery": true
    },
    "schema": `import com.orbitalhq.formats.Csv

// Common types
type Price inherits Double
type StockLevel inherits Int
type ProductName inherits String
type Category inherits String
type LastUpdated inherits Date
type WarehouseValue inherits Decimal

// Our CSV data source:
@Csv
closed model InventoryReport {

  // Defining some types in-line, which aren't
  // common / shared
  External_ID : ExternalId inherits String
  ProductTitle : ProductName
  CategoryType : Category
  Stock_Level : StockLevel
  UnitCost : Price
  VendorSKU_Code : VendorSKUCode inherits String
  Country_Origin_Code : CountryCode
  @Format("dd/MM/yyyy")
  DateUpdated : LastUpdated
}

service WarehouseS3Bucket {
  operation getInventoryReport():InventoryReport[]
}


// resolving country codes
type CountryName inherits String
type CountryCode inherits String

closed model CountryLookupResponse {
  name : CountryName
  code : CountryCode
}
service CountryService {
  // Service that takes a 2-letter country code,
  // and can provide data to resolve the country
  operation getCountry(CountryCode):CountryLookupResponse
}


// Finally, the schema of the data we want to save:
parameter model SavedStockReport {
  totalWarehouseValue: WarehouseValue
  goodsOnHand : {
      product : ProductName
      lastUpdated: LastUpdated
      price : Price
      stock: StockLevel
      // countryName : CountryName
  }[]
}
service StockReportsApi {
  write operation saveStockReport(SavedStockReport):StockReportUpdateResponse
}
closed model StockReportUpdateResponse {
  message: String
}



`,
    "query": `find {
    goodsOnHand:  InventoryReport[] as {
        product: ProductName
        @Format('dd-MMM-yy')
        lastUpdated: LastUpdated
        price: Price
        stock: StockLevel
    }[]
}
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getInventoryReport",
        "response": "External_ID,ProductTitle,CategoryType,Stock_Level,UnitCost,VendorSKU_Code,Country_Origin_Code,DateUpdated\n0daec88b-cde1-46e9-90ef-fd113b1592ea,Smartphone,Electronics,12,437.84,VSKU-97627,DE,07/01/2024\n3427f1ee-730f-455f-a73d-27f636e72bf7,Running Shoes,Clothing,56,34.62,VSKU-85111,CN,14/01/2024\n643c6b1a-61a3-4e90-9418-02884f4fbe4b,Bluetooth Speaker,Electronics,54,252.12,VSKU-60080,US,21/01/2024\nbe8a4da7-bf02-4bd8-ab56-15b33652e888,Coffee Maker,Home,54,409.5,VSKU-90406,CN,28/01/2024\nc24be0a3-0473-4253-bda5-384dd7882e69,Laptop,Electronics,87,383.13,VSKU-24786,IN,04/02/2024\n62ee9b40-3608-4180-9823-aa3170693237,Desk Chair,Home,20,59.85,VSKU-45202,US,11/02/2024\n88aa08db-5b5b-41dc-a1b2-160e535073e4,Noise Cancelling Headphones,Electronics,25,259.55,VSKU-33290,DE,18/02/2024\nb0d8e0f1-2473-4987-a165-a242a9b8d617,Novel - Fiction,Books,79,385.61,VSKU-60152,CN,25/02/2024\n4acf7722-7e84-4438-95cb-6efabb1df9ba,Action Figure,Toys,8,64.79,VSKU-73189,US,03/03/2024\n6b9d8b20-2328-41e2-b395-33ef0a4be190,LED Monitor,Electronics,22,270.89,VSKU-53634,US,10/03/2024"
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
              "body": "{ \"name\" : \"Germany\", \"code\" : \"DE\" }"
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
              "body": "{ \"name\" : \"China\", \"code\" : \"CN\" }"
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
              "body": "{ \"name\" : \"United States\", \"code\" : \"US\" }"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": "IN"
              }
            ],
            "response": {
              "body": "{ \"name\" : \"India\", \"code\" : \"IN\" }"
            }
          }
        ]
      },
      {
        "operationName": "saveStockReport",
        "response": "{\n    \"message\" : \"1 Row Inserted\"\n}"
      }
    ],
    "readme": "## Ingesting, transforming and aggregating CSV Data\nThis example shows working with CSV data - enriching and aggregating it.\n\n> [!TIP]\n> We've hidden the schema and diagram by default in this example. <br />\n> You can turn them back on in the sidebar\n\nOur goal is to take data an external data feed, normalize it,\nenich it, and do some calculations\n\nOur data is an InventoryReport from one of our warehouses, whose data is provided\nin a CSV file on an S3 bucket. (We've stubbed out the servers for this playground)\n\nHere's a query showing the raw data:\n\n```taxiql\nfind { InventoryReport[] }\n```\n\n### Converting to JSON\nOur goal is to transform this into a stock report in JSON.\n\nLet's start simple, by converting the CSV into JSON.\nTaxiQL assume JSON by default, so we can just ask for the \ndata we want:\n\n```taxiql\nfind {\n    goodsOnHand:  InventoryReport[] as {\n        product: ProductName\n        lastUpdated: LastUpdated\n        price: Price\n        stock: StockLevel\n    }[]\n}\n```\n\n### Handling date formats\nThe `LastUpdated` date on our source data is formatted as `10/03/2024`.\n\nTaxiQL has formatted this automatically back to ISO standard by default - `(YYYY-MM-DD)`. However, our target system\nactually needs month names, so let's apply a format:\n\n```taxiql\nfind {\n    goodsOnHand:  InventoryReport[] as {\n        product: ProductName\n        @Format('dd-MMM-yy')\n        lastUpdated: LastUpdated\n        price: Price\n        stock: StockLevel\n    }[]\n}\n```\n\n### Enriching the Country name\nOur data is providing a 2-letter country code - take a look:\n\n```taxiql\nimport CountryCode\nfind { InventoryReport[] } as  CountryCode[]\n```\n\nIn our report, we'd like this displayed with the full country name.\n\nLuckily, there's a REST API we can feed the country code to, to resolve it to a country name:\n\n```components\n{   \n   \"showTypeToolbar\" : false,\n   \"members\" : {\n      \"CountryCode\" : {},\n      \"CountryService\" : {},\n      \"CountryLookupResponse\" : {}\n   }\n}\n```\n\nHere's an example:\n\n```taxiql\ngiven { CountryCode = 'DE' }\nfind { CountryName }\n```\n\nThe TaxiQL engine can perform that enrichment on-the-fly, \nwe just have to ask for it.\n\nLet's update our query:\n\n```taxiql\n// After running this query, be sure to check the\n// Requests and Lineage Diagram panel,\n// to see the additional requests to the CountryAPI\nfind {\n    goodsOnHand:  InventoryReport[] as {\n        product: ProductName\n        @Format('dd-MMM-yy')        \n        lastUpdated: LastUpdated\n        price: Price\n        stock: StockLevel\n        // Add in the country name:\n        countryName: CountryName\n    }[]\n}\n```\n\n### Aggregating the total total value\nWe also want to know the total value of\ngoods on hand in the warehouse.\n\nSo, for each product, we need to calcualte\nthe `Price * StockLevel`, and sum those together.\n\nHere's how to ask for that in TaxiQL:\n\n```taxiql\nfind {\n    // Calculate the total warehouse value\n    // by summing the array, multiplying the \n    // StockLevel * Price\n    totalWarehouseValue: Decimal = InventoryReport[].sum((Price, StockLevel) -> Price * StockLevel)\n    goodsOnHand:  InventoryReport[] as {\n        product: ProductName\n        countryOfManufacture: CountryName\n        @Format('dd-MMM-yy')        \n        lastUpdated: LastUpdated\n        price: Price\n        stock: StockLevel\n    }[]\n}\n```\n\n\n## Writing the data somewhere\nNow that the data is in the format that we're after. we can \nsave it.\n\nThere's already a service declared to mock out a \ndata store (eg., could be MongoDB, Kafka, S3 or a DB).\n\n```taxi\n  service StockReportsApi {\n    write operation saveStockReport(SavedStockReport):StockReportUpdateResponse\n  }\n```\n\nTo call this, we simply add `call StockReportsApi::saveStockReport`\nto the end of our query:\n\n\n```taxiql\nfind {\n    totalWarehouseValue: WarehouseValue = InventoryReport[].sum((Price, StockLevel) -> Price * StockLevel)\n    goodsOnHand:  InventoryReport[] as {\n        product: ProductName\n        @Format('dd-MMM-yy')        \n        lastUpdated: LastUpdated\n        price: Price\n        stock: StockLevel\n        // Add in the country name:\n        countryName: CountryName\n    }[]\n}\n// Save the data into our data store\n// (Mongo / Postgres/ etc)\ncall StockReportsApi::saveStockReport\n```"
  }
}
