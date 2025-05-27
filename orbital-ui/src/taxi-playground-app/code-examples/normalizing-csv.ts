import {StubQueryMessageWithSlug} from "../../app/services/query.service";

// http://localhost:9500#pako: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
export const example: StubQueryMessageWithSlug = {
  "title": "Normalizing CSV",
  "slug": "normalizing-csv",
  "query": {
    "layout": {
      "showSchema": true,
      "showDiagram": true,
      "showReadme": false,
      "showQuery": true
    },
    "schema": `import com.orbitalhq.formats.Csv

namespace common {
  type Price inherits Double
  type StockLevel inherits Int
  type ProductName inherits String
  type Category inherits String
  type LastUpdated inherits Date

  type CountryName inherits String
  type CountryCode inherits String

  type BaseProductId inherits String
}

namespace countries {
  closed model CountryLookupResponse {
    name : CountryName
    code : CountryCode
  }
  service CountryService {
    operation getCountry(CountryCode):CountryLookupResponse
  }
}



namespace vendorA {
  type ProviderItemId inherits BaseProductId
  type VendorSKU inherits String

  @Csv
  closed model VendorAInventoryReport {
    Provider_Item_ID : ProviderItemId
    Product_Name : common.ProductName
    Category : common.Category
    Quantity_Available : common.StockLevel
    Price : common.Price
    Vendor_SKU : VendorSKU
    Country_of_Origin : CountryName
    Last_Updated : common.LastUpdated
  }

  service VendorAService {
    operation getVendorAInventoryReport():VendorAInventoryReport[]
  }
}

namespace vendorB {
  type ExternalId inherits BaseProductId
  type VendorSKUCode inherits String

  @Csv
  closed model VendorBInventoryReport {
    External_ID : ExternalId
    ProductTitle : common.ProductName
    CategoryType : common.Category
    Stock_Level : common.StockLevel
    UnitCost : common.Price
    VendorSKU_Code : VendorSKUCode
    Country_Origin_Code : CountryCode
    @Format("dd/MM/yyyy")
    DateUpdated : common.LastUpdated
  }

  service VendorAService {
    operation getVendorBInventoryReport():VendorBInventoryReport[]
  }

}


namespace com.acme {
  parameter model InventoryReport {
    itemId : BaseProductId
    country: CountryName
    price : Price
    lastUpdated: LastUpdated
  }

  service InventoryDatabase {
    table inventoryReport: InventoryReport[]

    write operation saveInventoryReport(InventoryReport[]):InventoryReport[]
  }
}`,
    "query": `find { VendorAInventoryReport[] }
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getVendorBInventoryReport",
        "response": "Provider_Item_ID,Product_Name,Category,Quantity_Available,Price,Last_Updated,Vendor_SKU,Country_of_Origin\n730e3ec0-9f09-47ca-9b06-0802c2b2666b,Smartphone,Electronics,72,157.35,2024-01-07,SKU-5373,India\n59032a0b-ae73-41c3-a46b-54af3f61cd97,Running Shoes,Clothing,91,200.15,2024-01-14,SKU-2293,China\n9a0fc06e-6ba8-48e0-b127-330dd32fd65b,Bluetooth Speaker,Electronics,49,477.28,2024-01-21,SKU-6447,China\n3a19f92a-6135-41ce-8684-d247e8b784d4,Coffee Maker,Home,24,16.34,2024-01-28,SKU-6000,USA\n32b1f4a7-add7-494c-8ad3-91df27d6b2fb,Laptop,Electronics,85,69.25,2024-02-04,SKU-8842,USA\nb02bcd6a-7e66-4974-9249-8f3fe1da81a2,Desk Chair,Home,62,248.93,2024-02-11,SKU-9190,India\n8786960a-5f62-442d-be0f-9fab4e28faf8,Noise Cancelling Headphones,Electronics,74,91.61,2024-02-18,SKU-9288,China\n4c039a10-80cd-4a57-895c-7f7119aada9c,Novel - Fiction,Books,30,239.94,2024-02-25,SKU-7316,China\n04157223-2355-406d-8332-5246ae9cc98a,Action Figure,Toys,78,448.32,2024-03-03,SKU-3420,Germany\n75f05eae-500b-4585-852d-c89382fa1ec9,LED Monitor,Electronics,19,100.78,2024-03-10,SKU-2277,China\n"
      },
      {
        "operationName": "getVendorAInventoryReport",
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
      }
    ],

    "readme": "## Normalizing CSV\nThis example shows working with CSV data - normalizing and enriching it.\n\nWe have two similar CSV formats - from VendorA and VendorB.\n\n```components\n{  \n   \"showTypeToolbar\" : false,\n   \"members\" : {\n      \"vendorA.VendorAInventoryReport\" : {},\n      \"vendorB.VendorBInventoryReport\" : {}\n   }\n}\n```\n\nThe data returned is similar, but different. Here's some TaxiQL queries to show the data:\n\n#### VendorA data:\n```taxiql\nfind { VendorAInventoryReport[] }\n```\n\n#### VendorB data:\n```taxiql\nfind { VendorBInventoryReport[] }\n```\n\nWe'd like to normalize this, and enrich it.\n\n## Queries:\n\n### Convert VendorA to standard format\n\n```taxiql\nfind { VendorAInventoryReport[] } as {\n    itemId : BaseProductId\n    country: CountryName\n    price : Price\n    lastUpdated: LastUpdated\n}[]\n```\n\n### Convert VendorB to standard format\n\n```taxiql\nfind { VendorBInventoryReport[] } as {\n    itemId : BaseProductId\n    country: CountryName\n    price : Price\n    lastUpdated: LastUpdated\n}[]\n```\n\n### Factor the common stuff into a type:\n\n```taxi\nmodel InventoryReport {\n  itemId : BaseProductId\n  country: CountryName\n  price : Price\n  lastUpdated: LastUpdated\n}\n```\n\nThen simplify our queries:\n\n```taxiql\nfind { VendorAInventoryReport[] } as InventoryReport[]\n```\n\n```taxiql\nfind { VendorBInventoryReport[] } as InventoryReport[]\n```\n\nFinally, declare a database to hold it:\n\n```taxi\nservice InventoryDatabase {\n  table inventoryReport: InventoryReport[]\n  write operation saveInventoryReport(InventoryReport[]):InventoryReport[]\n}\n```\n"
  }
}
