import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const UsingNestedFieldsInConstraints: StubQueryMessageWithSlug = {
  "title": "Using nested fields in constraints",
  "slug": "using-nested-fields-in-constraints",
  "query": {
    "schema": `type FilmId inherits String
type CustomerId inherits String
type RentalDate inherits Date

closed model FilmRental {
    filmId : FilmId
    customer : {
       id : CustomerId
    }
    rentalDate : RentalDate
}

service FilmRentalService {
    operation getByFilmId(filmId: FilmId): FilmRental(FilmId == filmId)
    operation getLatestRental(customerId: CustomerId): FilmRental(CustomerId == customerId)
}`,
    "query": `find {
    FilmRental(FilmId == '123')
} as (filmRental:FilmRental) -> {
    filmId,
    customer,
    previousRentals: FilmRental(CustomerId == filmRental.customer.id)
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
              "body": "{ \"filmId\" : \"123\" , \"customer\":{ \"id\":  \"B. A. Baracus\"} , \"rentalDate\": \"2024-12-16\"}"
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
              "body": "{ \"filmId\" : \"1112\" , \"customer\":{\"id\":  \"B. A. Baracus\"} , \"rentalDate\": \"2024-12-25\"}"
            }
          }
        ]
      }
    ],
    "readme": "# Nested Field Access in Constraints\n\nTaxiQL lets you reference nested fields in constraints to filter and enrich query results.\n\n## How It Works\n\nWhen writing constraints, you can:\n1. Access fields of referenced objects using dot notation\n2. Use these nested fields to filter related data\n3. Reference any level of nesting that's available in the source data\n\n## Example\n\n### Using Nested Fields in Constraints \n```taxiql\nfind {\n    FilmRental(FilmId == '123')\n} as (filmRental:FilmRental) -> {\n    filmId,\n    customer,\n    previousRentals: FilmRental(CustomerId == filmRental.customer.id)\n}\n```\n\n> [!NOTE]  \n> Be sure to check out the requests panel after running this query, to see what happened\n\n- Uses `filmRental.customer.id` to access a nested field\n- The nested field is used in a constraint to find related rentals\n- TaxiQL resolves the field path through the object hierarchy\n\nTaxiQL uses standard dot notation to traverse object relationships when evaluating constraints."
  }
}
