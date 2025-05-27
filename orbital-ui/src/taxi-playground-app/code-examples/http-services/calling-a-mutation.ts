import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const HttpMutation: StubQueryMessageWithSlug = {
  "title": "Http mutation",
  "slug": "http-mutation",
  "query": {
    "schema": `import taxi.http.RequestBody
// This example shows reading from one API, then transforming the
// data to pass to another, mutating API.

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
  // This operation is stubbed to return whatever is passed to it,
  // so you can see how the input is provided
  write operation updateCustomer(@RequestBody InternalCrmCustomer):InternalCrmCustomer
}`,
    "query": `import InternalCrmCustomer
import Customer

// This query starts by fetching data from one service with a find call...
find { Customer }

// ...Then calling another service with a call operation.
// The output from the query is transformed to the required
// input for the mutation operation.
// Note that field names differ in the input parameter
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
