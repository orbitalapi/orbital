import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const PolicyOnBaseType: StubQueryMessageWithSlug = {
  "title": "Policy on a base type (eg: PII data)",
  "slug": "policy-on-base-type",
  "query": {
    "schema": `import taxi.stdlib.concat
// This example shows restricting access to categories of
// data, using a data policy.
// We'll define the policy against an abstract concept -
// PII (short for Personally Identifiable Information)
// then apply that tag to multiple different types.
//
// By defining the policy against the PII type, it's enforced
// consistently wherever the PII tag is used


type PII
// Define some PII
type FirstName inherits String, PII
type LastName inherits String, PII


// Let's define a CustomerInfo object, returned from our API
model CustomerInfo {
  firstName : FirstName
  lastName : LastName
  // This is a concatenated field, joining multiple pieces
  // of PII together
  fullName : String = concat(FirstName, ' ', LastName)
}

// An API (or similar) exposing information about a customer.
// Note, we've configured a stub over in the stubs panel
// to return some data here.
service CustomerApi {
   operation getCustomers():CustomerInfo
}


// This is the User data, typically provided by your auth layer
type UserGroup inherits String
model UserInfo {
  roles:UserGroup[]
}


// Now secure it
// Anything that serves PII data (API calls, db queries, kafka streams)
// has this policy applied against it
policy MaskPiiData against PII (user: UserInfo) -> {
  read {
    when {
      // Admins see everything:
      user.roles.contains('ADMIN') -> PII
      // ...otherwise mask it
      else -> concat(left(PII,3), '****')
    }
  }
}`,
    "stubs": [
      {
        "operationName": "getCustomers",
        "response": "{\n   \"firstName\" : \"Jimmy\",\n   \"lastName\" : \"Sprockett\"\n}"
      }
    ],
    "query": `
// Try changing the role in the users auth and re-running the query
given { userInfo: UserInfo = { roles : ['ADMIN']}}
find { CustomerInfo }`,
    "parameters": {}
  }
}
