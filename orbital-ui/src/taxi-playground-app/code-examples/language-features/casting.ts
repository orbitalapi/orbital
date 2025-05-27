import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const Casting: StubQueryMessageWithSlug = {
  "title": "Casting",
  "slug": "casting",
  "query": {
    "schema": `closed model Person {
    id: PersonId inherits String
}
service PersonService {
    operation getPeople():Person[]
}`,
    "query": `find { Person[] } as {
   id : Long = (Long) PersonId
}[]
`,
    "parameters": {},
    "stubs": [
      {
        "operationName": "getPeople",
        "response": "[{\"id\" : \"123\"}]"
      }
    ],
    "readme": "# Type Casting in Queries\n\nTaxiQL supports explicit type casting in projections.\n\n## Examples\n\n### Without Casting\n```taxiql\nfind { Person[] }\n```\nReturns raw Person objects with their original types.\n\n### With Casting\n```taxiql\nfind { Person[] } as {\n   id : Long = (Long) PersonId\n}[]\n```\n- Cast syntax is `(TargetType) SourceType`\n- Useful when you need to change the type of a field in the result"
  }
}
