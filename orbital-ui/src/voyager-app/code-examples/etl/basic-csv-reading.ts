import {StubQueryMessageWithSlug} from "../../../app/services/query.service";
export const BasicCsvReading: StubQueryMessageWithSlug = {
    "title": "Basic CSV Reading",
    "slug": "basic-csv-reading",
    "query": {
        "layout": {
            "showSchema": true,
            "showDiagram": false,
            "showReadme": true,
            "showQuery": true
        },
        "schema": `import com.orbitalhq.formats.Csv

// Common types for our inventory data
type Price inherits Double
type StockLevel inherits Int
type ProductName inherits String
type Category inherits String

// Our CSV data source
@Csv
closed model InventoryReport {
    ProductTitle : ProductName
    CategoryType : Category
    Stock_Level : StockLevel
    UnitCost : Price
}

service WarehouseS3Bucket {
  operation getInventoryReport():InventoryReport[]
}`,
        "query": `find { InventoryReport[] }`,
        "parameters": {},
        "stubs": [
            {
                "operationName": "getInventoryReport",
                "response": "ProductTitle,CategoryType,Stock_Level,UnitCost\nSmartphone,Electronics,12,437.84\nRunning Shoes,Clothing,56,34.62\nBluetooth Speaker,Electronics,54,252.12\nCoffee Maker,Home,54,409.50"
            }
        ],
        "readme": `## Basic CSV Reading
This example demonstrates the basics of working with CSV data in TaxiQL.

Our sample data represents a simple inventory report with product information.

### Viewing Raw CSV Data
Let's start by looking at the raw CSV data:

\`\`\`taxiql
find { InventoryReport[] }
\`\`\`

### Converting to JSON
TaxiQL can automatically convert CSV to JSON. Let's restructure our data:

\`\`\`taxiql
find {
    InventoryReport[] as {
        product: ProductName
        category: Category
        stock: StockLevel
        price: Price
    }[]
}
\`\`\`

This query:
1. Reads the CSV data
2. Maps the columns to our desired structure
3. Returns it as JSON

Try modifying the query to select only specific fields or rename them differently!`
    }
}
