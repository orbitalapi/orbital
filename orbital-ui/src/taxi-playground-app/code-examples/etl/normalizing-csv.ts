import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const NormalizingCsv: StubQueryMessageWithSlug = {
  "title": "Normalizing Different CSV Formats",
  "slug": "csv-normalization",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": false,
      "showReadme": true,
      "showQuery": true
    },
    "schema": `import com.orbitalhq.formats.Csv

// Common types
type Price inherits Double
type StockLevel inherits Int
type ProductName inherits String
type LastUpdated inherits Date

// First warehouse format
@Csv
closed model WarehouseA {
    Product_Name : ProductName
    Stock_Level : StockLevel
    Unit_Price : Price
    @Format("dd/MM/yyyy")
    Last_Updated : LastUpdated
}

// Second warehouse format
@Csv
closed model WarehouseB {
    ItemName : ProductName
    Quantity : StockLevel
    Cost : Price
    @Format("yyyy-MM-dd")
    UpdateDate : LastUpdated
}

// Our target format
model StandardInventory {
    product : ProductName
    @Format('dd-MMM-yy')
    lastUpdated: LastUpdated
    price : Price
    stock: StockLevel
}

service WarehouseData {
    operation getWarehouseA():WarehouseA[]
    operation getWarehouseB():WarehouseB[]
}`,
    "query": `find {
    warehouseA: WarehouseA[] as StandardInventory[]
    warehouseB: WarehouseB[] as StandardInventory[]
}
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getWarehouseA",
        "response": "Product_Name,Stock_Level,Unit_Price,Last_Updated\nSmartphone,12,437.84,07/01/2024\nRunning Shoes,56,34.62,14/01/2024"
      },
      {
        "operationName": "getWarehouseB",
        "response": "ItemName,Quantity,Cost,UpdateDate\nLaptop,87,383.13,2024-02-04\nDesk Chair,20,59.85,2024-02-11"
      }
    ],
    "readme": "## Normalizing Different CSV Formats\nThis example shows how to handle data from different sources and normalize it into a standard format.\n\n### The Challenge\nWe have inventory data from two warehouses, each using different:\n- Column names\n- Date formats\n- Field naming conventions\n\n### View the Raw Data\nFirst, let's look at data from each warehouse:\n\n```taxiql\nfind { WarehouseA[] }\n```\n\n```taxiql\nfind { WarehouseB[] }\n```\n\n### Creating a Standard Format\nWe'll define a standard format and map both sources to it:\n\n```taxi\n// Our target format\nmodel StandardInventory {\n    product : ProductName\n    @Format('dd-MMM-yy')\n    lastUpdated: LastUpdated\n    price : Price\n    stock: StockLevel\n}\n```\n\nTo convert the source data to the target format is a simple query:\n\n\n```taxiql\nfind {\n    warehouseA: WarehouseA[] as StandardInventory[]\n    warehouseB: WarehouseB[] as StandardInventory[]\n}\n```\n\nTaxiQL automatically handles:\n- Column name mapping\n- Date format conversion\n- Type conversion\n\nTry modifying the StandardInventory model to change how the data is normalized!"
  }
}
