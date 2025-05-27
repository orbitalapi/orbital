import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const QueryingWithCriteria: StubQueryMessageWithSlug = {
  "title": "Query criteria and operation response contracts",
  "slug": "querying-with-criteria",
  "query": {
    "schema": `// Person model
closed model Person {
    firstName : FirstName inherits String
    lastName: LastName inherits String
    age : Age inherits Int
}

// Service used to get and filter People
service PeopleService {
   // Matches because it returns Person[] with no constraints
   operation getAllPeople(): Person[]

   // Matches because the response contract matches the query constraint
   operation getPeopleByAge(age: Age): Person[](Age == age)

   // Operation contract with multiple constraints
   operation findPeopleInAgeRange(min: Age, max: Age): Person[](
      Age >= min && Age <= max
   )

   // Equivalent shorthand using spread operator
   operation findPerson(FirstName, LastName): Person[](...)
}`,
    "query": `find { Person[](FirstName == 'John' && LastName == 'Doe') }
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getAllPeople",
        "response": "[\n  {\"firstName\": \"John\",\"lastName\":\"Doe\", \"age\": 25},\n  {\"firstName\": \"Joe\",\"lastName\":\"Bloggs\", \"age\": 55}\n]"
      },
      {
        "operationName": "getPeopleByAge",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "age",
                "value": 25
              }
            ],
            "response": {
              "body": "[\n  {\"firstName\": \"John\",\"lastName\":\"Doe\", \"age\": 25}\n]"
            }
          },
          {
            "inputs": [
              {
                "name": "age",
                "value": 55
              }
            ],
            "response": {
              "body": "[\n    {\"firstName\": \"Joe\",\"lastName\":\"Bloggs\", \"age\": 55}\n]"
            }
          }
        ]
      },
      {
        "operationName": "findPeopleInAgeRange",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "min",
                "value": 20
              },
              {
                "name": "max",
                "value": 30
              }
            ],
            "response": {
              "body": "[\n  {\"firstName\": \"John\",\"lastName\":\"Doe\", \"age\": 25}\n]"
            }
          },
          {
            "inputs": [
              {
                "name": "min",
                "value": 50
              },
              {
                "name": "max",
                "value": 60
              }
            ],
            "response": {
              "body": "[\n  {\"firstName\": \"Joe\",\"lastName\":\"Bloggs\", \"age\": 55}\n]"
            }
          }
        ]
      },
      {
        "operationName": "findPerson",
        "response": "",
        "conditionalResponses": [
          {
            "inputs": [
              {
                "name": "p0",
                "value": "Joe"
              },
              {
                "name": "p1",
                "value": "Bloggs"
              }
            ],
            "response": {
              "body": "[\n  {\"firstName\": \"Joe\",\"lastName\":\"Bloggs\", \"age\": 55}\n]"
            }
          },
          {
            "inputs": [
              {
                "name": "p0",
                "value": "John"
              },
              {
                "name": "p1",
                "value": "Doe"
              }
            ],
            "response": {
              "body": "[\n  {\"firstName\": \"John\",\"lastName\":\"Doe\", \"age\": 25}\n]"
            }
          }
        ]
      }
    ],
    "readme": "# Query criteria and operation response contracts\n\nOperation response contracts define how queries are matched to service operations. This matching process is fundamental to how Taxi discovers data.\n\n## Basic Operation Matching\n\nThe simplest query requests all records of a type:\n\n```taxiql\nfind { Person[] }\n```\n\nThis query matches operations that return `Person[]` with no constraints:\n\n```taxi\nservice PeopleService {\n   // Matches because it returns Person[] with no constraints\n   operation getAllPeople(): Person[]\n   \n   // Does not match because it has constraints\n   operation getPeopleByAge(age: Age): Person[](Age == age)\n}\n```\n\n## Adding Query Constraints\n\n> [!NOTE]  \n> Service responses are stubbed in the playground.\n> You can run these examples, but changing the criteria won't affect what's returned\n\n\nWhen a query includes constraints, Taxi matches it to operations with compatible response contracts:\n\n```taxiql\n// Query with an age constraint\nfind { Person[](Age == 25) }\n```\n\nThis matches operations whose response contract supports the constraint:\n\n```taxi\nservice PeopleService {\n   // Matches because the response contract matches the query constraint\n   operation getPeopleByAge(age: Age): Person[](Age == age)\n   \n   // Does not match - different constraint\n   operation getPeopleByName(name: FirstName): Person[](FirstName == name)\n}\n```\n\n## Complex Constraint Matching\n\nOperations can declare multiple constraints that queries must satisfy:\n\n```taxi\nservice PeopleService {\n   // Operation contract with multiple constraints\n   operation findPeopleInAgeRange(min: Age, max: Age): Person[](\n      Age >= min && Age <= max\n   )\n}\n```\n\nThe corresponding query must match all constraints:\n\n```taxiql\n// Matches the findPeopleInAgeRange operation\nfind { Person[](Age >= 50 && Age <= 60) }\n```\n\n## Shorthand Contract Syntax\n\nFor operations that match exact values, use the spread operator (...) as shorthand:\n\n```taxi\nservice PeopleService {\n   // Long form\n   operation findPerson(firstName: FirstName, lastName: LastName): Person[](\n      FirstName == firstName &&\n      LastName == lastName\n   )\n   \n   // Equivalent shorthand using spread operator\n   operation findPerson(FirstName, LastName): Person[](...)\n}\n```\n\nBoth operations match the same query:\n\n```taxiql\nfind { Person[](FirstName == 'John' && LastName == 'Doe') }\n```\n\nTaxi determines which service operations to call based on the query constraints.\n\nThis declarative approach lets you focus on the data you want rather than how to get it. \n\n"
  }
}
