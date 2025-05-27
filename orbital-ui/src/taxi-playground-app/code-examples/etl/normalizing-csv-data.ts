import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

// Keeping this for reference,
// but this example got split down into smaller examples
/**
 * @deprecated
 */
export const NormalizingCsvData: StubQueryMessageWithSlug = {
  "title": "Normalizing CSV Data",
  "slug": "normalizing-csv-data",
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
type Category inherits String
type LastUpdated inherits Date

// Our CSV data source:
@Csv
closed model MelbourneInventoryReport {

  // Defining some types in-line, which aren't
  // common / shared
  External_ID : ExternalId inherits String
  ProductTitle : ProductName
  CategoryType : Category
  Stock_Level : StockLevel
  UnitCost : Price
  VendorSKU_Code : VendorSKUCode inherits String
  @Format("dd/MM/yyyy")
  DateUpdated : LastUpdated
}

  @Csv
  closed model SydneyInventoryReport {
    Provider_Item_ID : ExternalId
    Product_Name : ProductName
    Category : Category
    Quantity_Available : StockLevel
    Price : Price
    Vendor_SKU : VendorSKUCode
    Last_Updated : LastUpdated
  }


service WarehouseS3Bucket {
  operation getMelbourneInventoryReport():MelbourneInventoryReport[]
  operation getSydneyInventoryReport():SydneyInventoryReport[]
}

// Finally, the schema of the data we want to save:
model StockReport {
    product : ProductName
    @Format('dd-MMM-yy')
    lastUpdated: LastUpdated
    price : Price
    stock: StockLevel
}



`,
    "query": `find { MelbourneInventoryReport[] } as StockReport[]
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getMelbourneInventoryReport",
        "response": "External_ID,ProductTitle,CategoryType,Stock_Level,UnitCost,VendorSKU_Code,Country_Origin_Code,DateUpdated\n0daec88b-cde1-46e9-90ef-fd113b1592ea,Smartphone,Electronics,12,437.84,VSKU-97627,DE,07/01/2024\n3427f1ee-730f-455f-a73d-27f636e72bf7,Running Shoes,Clothing,56,34.62,VSKU-85111,CN,14/01/2024\n643c6b1a-61a3-4e90-9418-02884f4fbe4b,Bluetooth Speaker,Electronics,54,252.12,VSKU-60080,US,21/01/2024\nbe8a4da7-bf02-4bd8-ab56-15b33652e888,Coffee Maker,Home,54,409.5,VSKU-90406,CN,28/01/2024\nc24be0a3-0473-4253-bda5-384dd7882e69,Laptop,Electronics,87,383.13,VSKU-24786,IN,04/02/2024\n62ee9b40-3608-4180-9823-aa3170693237,Desk Chair,Home,20,59.85,VSKU-45202,US,11/02/2024\n88aa08db-5b5b-41dc-a1b2-160e535073e4,Noise Cancelling Headphones,Electronics,25,259.55,VSKU-33290,DE,18/02/2024\nb0d8e0f1-2473-4987-a165-a242a9b8d617,Novel - Fiction,Books,79,385.61,VSKU-60152,CN,25/02/2024\n4acf7722-7e84-4438-95cb-6efabb1df9ba,Action Figure,Toys,8,64.79,VSKU-73189,US,03/03/2024\n6b9d8b20-2328-41e2-b395-33ef0a4be190,LED Monitor,Electronics,22,270.89,VSKU-53634,US,10/03/2024"
      },
      {
        "operationName": "getSydneyInventoryReport",
        "response": "Provider_Item_ID,Product_Name,Category,Quantity_Available,Price,Last_Updated,Vendor_SKU,Country_of_Origin\n730e3ec0-9f09-47ca-9b06-0802c2b2666b,Smartphone,Electronics,72,157.35,2024-01-07,SKU-5373,India\n59032a0b-ae73-41c3-a46b-54af3f61cd97,Running Shoes,Clothing,91,200.15,2024-01-14,SKU-2293,China\n9a0fc06e-6ba8-48e0-b127-330dd32fd65b,Bluetooth Speaker,Electronics,49,477.28,2024-01-21,SKU-6447,China\n3a19f92a-6135-41ce-8684-d247e8b784d4,Coffee Maker,Home,24,16.34,2024-01-28,SKU-6000,USA\n32b1f4a7-add7-494c-8ad3-91df27d6b2fb,Laptop,Electronics,85,69.25,2024-02-04,SKU-8842,USA\nb02bcd6a-7e66-4974-9249-8f3fe1da81a2,Desk Chair,Home,62,248.93,2024-02-11,SKU-9190,India\n8786960a-5f62-442d-be0f-9fab4e28faf8,Noise Cancelling Headphones,Electronics,74,91.61,2024-02-18,SKU-9288,China\n4c039a10-80cd-4a57-895c-7f7119aada9c,Novel - Fiction,Books,30,239.94,2024-02-25,SKU-7316,China\n04157223-2355-406d-8332-5246ae9cc98a,Action Figure,Toys,78,448.32,2024-03-03,SKU-3420,Germany\n75f05eae-500b-4585-852d-c89382fa1ec9,LED Monitor,Electronics,19,100.78,2024-03-10,SKU-2277,China\n"
      }
    ],
    "readme": "## Normalizing CSV data\nThis example shows working with CSV data from two different sources, and normalizing it.\n\n> [!TIP]\n> We've hidden the schema and diagram by default in this example. <br />\n> You can turn them back on in the sidebar\n\nOur goal is to take data from two different external feeds and normalize it into a standard format.\n\nOur data is InventoryReports from two of our warehouses - one in Sydney, one in Melbourne, \nwhose data is provided in a CSV files, but with different formats.\n\nHere's a query showing the raw data:\n\n```taxiql\nfind { MelbourneInventoryReport[] }\n```\n\n```taxiql\nfind { SydneyInventoryReport[] }\n```\n\nThere's a couple of key differences:\n * Date formats differ\n * Column names are different\n\n### Converting to JSON\nOur goal is to transform this into a stock report in JSON.\n\nLet's start simple, by converting the CSV into JSON.\nTaxiQL assume JSON by default, so we can just ask for the \ndata we want:\n\n```taxiql\nfind { MelbourneInventoryReport[] } as {\n    product: ProductName\n    lastUpdated: LastUpdated\n    price: Price\n    stock: StockLevel\n}[]\n```\n\n### Handling date formats\nThe Melbourne data is prodiving the `LastUpdated` date formatted as `10/03/2024`.\n\nTaxiQL has formatted this automatically back to ISO standard by default - `(YYYY-MM-DD)`. However, our target system\nactually needs month names, so let's apply a format:\n\n```taxiql\nfind { MelbourneInventoryReport[] } as {\n    product: ProductName\n    @Format('dd-MMM-yy')\n    lastUpdated: LastUpdated\n    price: Price\n    stock: StockLevel\n}[]\n```\n\n### Extracting our target schema\nOur data looks right, so lets extract this into it's own schema.\n\nThis isn't strictly neccessary, but as we want to use this as the target\nfor multiple different sources, it keeps our code DRY.\n\n```taxi\nmodel StockReport {\n    product : ProductName\n    @Format('dd-MMM-yy')\n    lastUpdated: LastUpdated\n    price : Price\n    stock: StockLevel\n}\n```\n\nNow we can use this to normalize both our sources:\n\n```taxiql\nfind { MelbourneInventoryReport[] } as StockReport[]\n```\n\n```taxiql\nfind { SydneyInventoryReport[] } as StockReport[]\n```"
  }
}
