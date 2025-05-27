import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const HttpMutationWithProjection: StubQueryMessageWithSlug = {
  "title": "Http mutation with projection",
  "slug": "http-mutation-with-projection",
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
}

model CustomerName {
  given_name : FirstName
  surname : LastName
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
    "query": `// 1. Initial Query
// Fetch a Customer record
find { Customer } as {
    // 2. Projection Stage
    // Transform the Customer data before passing to mutation:

    // Create CustomerName[] array for InternalCrm input
    // Uses listOf() to convert single value to array
    customerNames: listOf(CustomerName)

    // Include all other Customer fields using spread
    // Allows flexibility in what's passed through
    ...
}

// 3. Mutation Stage
// Take projected data and call InternalCrm::updateCustomer
// Field names automatically mapped between projection and mutation input
call InternalCrm::updateCustomer`,
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
      }
    ],
    "readme": ""
  }
}
