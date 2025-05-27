import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const EnrichingDataWithLoading: StubQueryMessageWithSlug = {
  "title": "Enriching by loading external data",
  "slug": "enriching-loading-external-data",
  "query": {
    "schema": `type FilmId inherits String
type CustomerId inherits String
type RentalDate inherits Date

closed model FilmRental {
    filmId : FilmId
    customerId : CustomerId
    rentalDate : RentalDate
}

service FilmRentalService {
    operation getByFilmId(filmId: FilmId): FilmRental(FilmId == filmId)
    operation getPreviousRentals(customerId: CustomerId): FilmRental[](CustomerId == customerId)
    operation getLatestRental(customerId: CustomerId): FilmRental(CustomerId == customerId)
}`,
    "query": `find {
    FilmRental(FilmId == '123')
} as (filmRental:FilmRental) -> {
    filmId,
    customerId,
    previousRentals: FilmRental(filmRental.customerId == CustomerId)
}
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getByFilmId",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "filmId",
                "value": "123"
              }
            ],
            "response": {
              "body": "{ \"filmId\" : \"123\" , \"customerId\"  :  \"B. A. Baracus\" , \"rentalDate\": \"2024-12-16\"}"
            }
          }
        ]
      },
      {
        "operationName": "getPreviousRentals",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "customerId",
                "value": "B. A. Baracus"
              }
            ],
            "response": {
              "body": "[{ \"filmId\" : \"456\" , \"customerId\"  :  \"B. A. Baracus\" , \"rentalDate\": \"2024-11-01\"},{ \"filmId\" : \"789\" , \"customerId\"  :  \"B. A. Baracus\" , \"rentalDate\": \"2024-09-25\"}]"
            }
          }
        ]
      },
      {
        "operationName": "getLatestRental",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "customerId",
                "value": "B. A. Baracus"
              }
            ],
            "response": {
              "body": "{ \"filmId\" : \"1112\" , \"customerId\"  :  \"B. A. Baracus\" , \"rentalDate\": \"2024-12-25\"}"
            }
          }
        ]
      }
    ],
    "readme": "# Query-based Data Enrichment\n\nTaxiQL enriches data by calling operations that match the types and constraints in your query. It uses return types (single vs array) and constraint parameters to determine which operations to call.\n\n## How It Works\n\nWhen TaxiQL needs to enrich data in a query, it:\n1. Identifies the type of data needed (single item or array)\n2. Checks available constraints\n3. Finds operations that can return the required data\n4. Calls operations that match these requirements\n\n## Examples\n\n### Fetching an array \nThis example shows how TaxiQL enriches data by fetching multiple related records:\n\n```taxiql\nfind {\n   FilmRental(FilmId == '123')\n} as (filmRental:FilmRental) -> {\n   filmId,\n   customerId,\n   previousRentals: FilmRental[](CustomerId == filmRental.customerId)\n}\n```\n- Requests related rentals as an array using `FilmRental[]`\n- Uses `customerId` as a constraint to find related records\n- TaxiQL calls operations that return multiple rentals\n\n### Fetching a single value\nThis example demonstrates enriching with a single related record:\n\n```taxiql\nfind {\n    FilmRental(FilmId == '123')\n} as (filmRental:FilmRental) -> {\n    filmId,\n    customerId,\n    previousRentals: FilmRental(CustomerId == filmRental.customerId)\n}\n```\n- Requests a single related rental\n- Uses the same customer ID constraint\n- TaxiQL calls operations that return a single rental\n\n### Flexible Constraint Matching\nThis example shows how constraints can be specified in different orders:\n\n```taxiql\nfind {\n    FilmRental(FilmId == '123')\n} as (filmRental:FilmRental) -> {\n    filmId,\n    customerId,\n    previousRentals: FilmRental(filmRental.customerId == CustomerId)\n}\n```\n- Constraints can be written in either order\n- TaxiQL matches constraints to available operation parameters\n\nTaxiQL uses constraints and return types to find and call the appropriate operations for enriching query results."
  }
}
