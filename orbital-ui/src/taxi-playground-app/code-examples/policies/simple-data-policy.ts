import {StubQueryMessageWithSlug} from "../../../app/services/query.service";

export const SimpleDataPolicy: StubQueryMessageWithSlug = {
  "title": "Restricting a field",
  "slug": "policy-restricting-a-field",
  "query": {
    "schema": `// This example shows restricting access to sensitive data
// by using a data policy.
// We'll define a policy that restricts access to an employee's
// salary data


// First, let's define Employee info, containing data we want to secure
model EmployeeInfo {
  name : Name inherits String
  salary : Salary inherits Int
}

// An API (or similar) exposing information about an employee.
// Note, we've configured a stub over in the stubs panel
// to return some data here.
service EmployeeService {
   operation getEmployee():EmployeeInfo
}


// This is the User data, typically provided by your auth layer
type UserGroup inherits String
model UserInfo {
  groups:UserGroup[]
}

// Data policy
policy OnlyManagers against EmployeeInfo (userInfo : UserInfo) -> {
   read {
      when {
         // Managers can see everything
         userInfo.groups.contains('Manager') -> EmployeeInfo
         // Users can see everything except salary
         userInfo.groups.contains('User') -> EmployeeInfo as {
            ... except { salary }
          }
         // Everyone else gets an error
         else -> throw((NotAuthorizedError) { message: 'Not Authorized' })
      }
   }
}`,
    "stubs": [
      {
        "operationName": "getEmployee",
        "response": "{\n   \"name\" : \"Jimmy\",\n   \"salary\" : 30000\n}"
      }
    ],
    "query": `
// Try changing the 'Manager' role to 'User' and re-running the query
// Then try with a different value altogether
given { userInfo: UserInfo = { groups : ['Manager']}}
find { EmployeeInfo }`,
    "parameters": {}
  }
}
