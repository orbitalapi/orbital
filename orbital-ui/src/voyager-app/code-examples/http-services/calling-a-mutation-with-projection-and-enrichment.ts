import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const MutationWithProjectionAndEnrichment: StubQueryMessageWithSlug = {
  "title": "Http mutation with projection and enrichment",
  "slug": "http-mutation-with-projection-and-enrichment",
  "query": {
    "schema": `import taxi.http.RequestBody
// This example shows reading from one API, then transforming the
// data to pass to another, mutating API.
// In this example, we need to handle nesting a value into an array,
// which is a transformation not handled by default

type CustomerId inherits String
type FirstName inherits String
type LastName inherits String

// This is a closed model because it's provided as a response
// from an API.
closed model Customer {
  customerId : CustomerId
  firstName : FirstName
  lastName : LastName
}

// This is a closed model because it's provided as a response from an API
// and a parameter model because it's also an input to an API
closed parameter model InternalCrmCustomer {
  customer_id : CustomerId

  // Our internal CRM
  // has names in an array.
  names: CustomerName[]

  pointsBalance: PointsBalance
}

model CustomerName {
  given_name : FirstName
  surname : LastName
}

closed model LoyaltyPoints {
  id : CustomerId
  points : PointsBalance inherits Int
}

service LoyaltyPointsApi {
  operation getPoints(CustomerId):LoyaltyPoints
}

service ExternalCrm {
  @HttpOperation(url = "https://someUrl.com/customers", method = "GET")
  operation getCustomer():Customer
}

service InternalCrm {
  @HttpOperation(url = "http://internalCrm/customers", method = "POST")
  // Note that this is declared as a write operation.
  // Write operations are invoked using the 'call' statement
  write operation updateCustomer(@RequestBody InternalCrmCustomer):InternalCrmCustomer
}`,
    "query": `find { Customer } as {
   names : listOf(CustomerName)
   ...
}
call InternalCrm::updateCustomer
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getCustomer",
        "response": "{\n  \"customerId\" : \"abc\",\n  \"firstName\" : \"Jimmy\",\n  \"lastName\" : \"Schmitt\"\n}"
      },
      {
        "operationName": "updateCustomer",
        "response": "",
        "echoInput": true
      },
      {
        "operationName": "getPoints",
        "response": "{ \n  \"id\" : \"abc\", \n  \"points\": 2300\n}"
      }
    ],
    "readme": "## HTTP Mutation with Enrichment\n\nThis example demonstrates combining data from multiple HTTP services and enriching it before submission to another service.\n\nWe'll use three services:\n- ExternalCRM: Provides customer data\n- LoyaltyPointsApi: Manages points balances\n- InternalCRM: Our target system for writing enriched data\n\n```components\n{\n   \"members\" : {\n     \"InternalCrm\" : {},\n     \"ExternalCrm\" : {},\n     \"LoyaltyPointsApi\" : {}\n   }\n}\n```\n\nLet's examine the source data:\n\n```taxiql\nfind { Customer }\n```\n\nHere's our target model:\n\n```taxi\nclosed parameter model InternalCrmCustomer {\n  customer_id : CustomerId\n  names: CustomerName[]  // Names stored as array\n  pointsBalance: PointsBalance\n}\n```\n\nWe need to:\n1. Transform customer names into an array\n2. Enrich data with loyalty points\n\n### Enriching with Points Data\n\nThe query engine automatically handles enrichment by calling `LoyaltyPointsApi` using the `CustomerId` from our source data:\n\n```taxiql\nfind { Customer } as {\n   points: LoyaltyPoints\n   ...\n}\n```\n\n### Creating the Names Array\n\nTransforming into and out of arrays isn't handled automatically.\nTo nest names in an array, we use the `listOf` function:\n\n```taxiql\nfind { Customer } as {\n   names : listOf(CustomerName)\n   ...\n}\n```\n\n### Executing the Mutation\n\nOur target operation is defined as:\n\n```taxi\nservice InternalCrm {\n  @HttpOperation(url = \"http://internalCrm/customers\", method = \"POST\")\n  write operation updateCustomer(@RequestBody InternalCrmCustomer):InternalCrmCustomer\n}\n```\n\nExecute the mutation using the `call` operator:\n\n```taxiql\nfind { Customer } as {\n   names : listOf(CustomerName)\n   ...\n}\ncall InternalCrm::updateCustomer\n```\n\nNote that we've had to explicitly define the nesting of data into an array, but the\nfetching and enriching of loyalty points was handled automatically for yus. \n\n\n### How Mutations Work\n\n1. **Context Gathering**: The mutation starts with data from the previous operation (in our case, the projected query result)\n2. **Parameter Construction**: The engine builds the input parameter by:\n   - Matching fields by semantic type\n   - Performing necessary transformations\n   - Enriching with data from other services\n3. **Execution**: The constructed parameter is sent to the write operation\n\nThe query engine handles most transformations automatically. Only specify transformations when the engine needs explicit guidance, like with the `listOf` function above."
  }
}
