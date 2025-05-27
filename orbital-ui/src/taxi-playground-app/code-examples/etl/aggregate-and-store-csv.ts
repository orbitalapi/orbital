import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const AggregateAndStoreCsv: StubQueryMessageWithSlug = {
  "title": "Aggregating and Saving CSV Data",
  "slug": "csv-aggregation-storage",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": false,
      "showReadme": true,
      "showQuery": true
    },
    "schema": `import com.orbitalhq.formats.Csv

type Price inherits Decimal
type StockLevel inherits Int
type ProductName inherits String
type TotalValue inherits Decimal

@Csv
closed model InventoryReport {
    ProductTitle : ProductName
    Stock_Level : StockLevel
    UnitCost : Price
}

// Output format for our calculated report
parameter model InventorySummary {
    totalValue: TotalValue
    items: {
        product: ProductName
        price: Price
        stock: StockLevel
        itemValue: TotalValue
    }[]
}

service WarehouseData {
    operation getInventory():InventoryReport[]
}

service ReportingSystem {
    write operation saveReport(InventorySummary):SaveResponse
}

closed model SaveResponse {
    message: String
}`,
    "query": `find {
    totalValue: TotalValue = InventoryReport[].sum((Price, StockLevel) -> Price * StockLevel)
    items: InventoryReport[] as {
        product: ProductName
        price: Price
        stock: StockLevel
        itemValue: TotalValue = Price * StockLevel
    }[]
}
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getInventory",
        "response": "ProductTitle,Stock_Level,UnitCost\nSmartphone,12,437.84\nRunning Shoes,56,34.62\nBluetooth Speaker,54,252.12"
      },
      {
        "operationName": "saveReport",
        "response": "{ \"message\": \"Report saved successfully\" }"
      }
    ],
    "readme": "## Aggregating and Saving CSV Data\nThis example shows how to perform calculations on CSV data and save the results.\n\n### The Goal\nCalculate the total value of inventory and save a summary report.\n\n### View the Raw Data\nLet's look at our inventory:\n\n```taxiql\nfind { InventoryReport[] }\n```\n\n### Calculating Values\nWe'll calculate total value for each item and overall:\n\n```taxiql\nfind {\n    totalValue: TotalValue = InventoryReport[].sum((Price, StockLevel) -> Price * StockLevel)\n    items: InventoryReport[] as {\n        product: ProductName\n        price: Price\n        stock: StockLevel\n        itemValue: TotalValue = Price * StockLevel\n    }[]\n}\n```\n\n### Saving the Report\nAdd the save operation to persist our calculations:\n\n```taxiql\nfind {\n    totalValue: TotalValue = InventoryReport[].sum((Price, StockLevel) -> Price * StockLevel)\n    items: InventoryReport[] as {\n        product: ProductName\n        price: Price\n        stock: StockLevel\n        itemValue: TotalValue = Price * StockLevel\n    }[]\n}\ncall ReportingSystem::saveReport\n```\n\nThe calculation steps:\n1. Calculate per-item values\n2. Sum all items for total\n3. Format into report structure\n4. Save to reporting system"
  }
}
