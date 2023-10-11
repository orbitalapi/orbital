export const PARSE_RESULT = {
  "newTypes": [
    {
      "name": {
        "fullyQualifiedName": "Person",
        "parameters": [],
        "name": "Person",
        "parameterizedName": "Person",
        "namespace": "",
        "shortDisplayName": "Person",
        "longDisplayName": "Person"
      },
      "attributes": {
        "firstName": {
          "type": {
            "fullyQualifiedName": "FirstName",
            "parameters": [],
            "name": "FirstName",
            "parameterizedName": "FirstName",
            "namespace": "",
            "shortDisplayName": "FirstName",
            "longDisplayName": "FirstName"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "FirstName",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "lastName": {
          "type": {
            "fullyQualifiedName": "LastName",
            "parameters": [],
            "name": "LastName",
            "parameterizedName": "LastName",
            "namespace": "",
            "shortDisplayName": "LastName",
            "longDisplayName": "LastName"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "LastName",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        }
      },
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "<unknown>",
          "version": "0.0.0",
          "content": "model Person {\n            firstName : FirstName inherits String\n            lastName : LastName inherits String\n         }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "dummy",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/dummy",
            "id": "com.orbitalhq/dummy/0.1.0",
            "uriSafeId": "com.orbitalhq:dummy:0.1.0"
          },
          "language": "taxi",
          "packageQualifiedName": "[com.orbitalhq/dummy/0.1.0]/<unknown>",
          "id": "<unknown>:0.0.0",
          "contentHash": "9da54c",
          "fullHash": "dc00f4ed096680fa509af25f09fa59194e8473d94cd7d42bd66f538c89450481"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "Person",
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "Person",
      "fullyQualifiedName": "Person",
      "memberQualifiedName": {
        "fullyQualifiedName": "Person",
        "parameters": [],
        "name": "Person",
        "parameterizedName": "Person",
        "namespace": "",
        "shortDisplayName": "Person",
        "longDisplayName": "Person"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "Actor",
        "parameters": [],
        "name": "Actor",
        "parameterizedName": "Actor",
        "namespace": "",
        "shortDisplayName": "Actor",
        "longDisplayName": "Actor"
      },
      "attributes": {
        "firstName": {
          "type": {
            "fullyQualifiedName": "FirstName",
            "parameters": [],
            "name": "FirstName",
            "parameterizedName": "FirstName",
            "namespace": "",
            "shortDisplayName": "FirstName",
            "longDisplayName": "FirstName"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "FirstName",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "lastName": {
          "type": {
            "fullyQualifiedName": "LastName",
            "parameters": [],
            "name": "LastName",
            "parameterizedName": "LastName",
            "namespace": "",
            "shortDisplayName": "LastName",
            "longDisplayName": "LastName"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "LastName",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "agent": {
          "type": {
            "fullyQualifiedName": "Person",
            "parameters": [],
            "name": "Person",
            "parameterizedName": "Person",
            "namespace": "",
            "shortDisplayName": "Person",
            "longDisplayName": "Person"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "Person",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        }
      },
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "Person",
          "parameters": [],
          "name": "Person",
          "parameterizedName": "Person",
          "namespace": "",
          "shortDisplayName": "Person",
          "longDisplayName": "Person"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "<unknown>",
          "version": "0.0.0",
          "content": "model Actor inherits Person {\n               agent : Person\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "dummy",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/dummy",
            "id": "com.orbitalhq/dummy/0.1.0",
            "uriSafeId": "com.orbitalhq:dummy:0.1.0"
          },
          "language": "taxi",
          "packageQualifiedName": "[com.orbitalhq/dummy/0.1.0]/<unknown>",
          "id": "<unknown>:0.0.0",
          "contentHash": "fdefee",
          "fullHash": "86bfc43e0717a5b94a096a2e40d65d4dcac48a41ad295d993a61ee1ba1e6ee66"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "Actor",
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "Actor",
      "fullyQualifiedName": "Actor",
      "memberQualifiedName": {
        "fullyQualifiedName": "Actor",
        "parameters": [],
        "name": "Actor",
        "parameterizedName": "Actor",
        "namespace": "",
        "shortDisplayName": "Actor",
        "longDisplayName": "Actor"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "Movie",
        "parameters": [],
        "name": "Movie",
        "parameterizedName": "Movie",
        "namespace": "",
        "shortDisplayName": "Movie",
        "longDisplayName": "Movie"
      },
      "attributes": {
        "actors": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "Actor",
                "parameters": [],
                "name": "Actor",
                "parameterizedName": "Actor",
                "namespace": "",
                "shortDisplayName": "Actor",
                "longDisplayName": "Actor"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<Actor>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Actor[]",
            "longDisplayName": "Actor[]"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "Actor[]",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "director": {
          "type": {
            "fullyQualifiedName": "Person",
            "parameters": [],
            "name": "Person",
            "parameterizedName": "Person",
            "namespace": "",
            "shortDisplayName": "Person",
            "longDisplayName": "Person"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "Person",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "Title",
            "namespace": "",
            "shortDisplayName": "Title",
            "longDisplayName": "Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        }
      },
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "<unknown>",
          "version": "0.0.0",
          "content": "model Movie {\n            actors : Actor[]\n            director : Person\n            title : Title inherits String\n         }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "dummy",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/dummy",
            "id": "com.orbitalhq/dummy/0.1.0",
            "uriSafeId": "com.orbitalhq:dummy:0.1.0"
          },
          "language": "taxi",
          "packageQualifiedName": "[com.orbitalhq/dummy/0.1.0]/<unknown>",
          "id": "<unknown>:0.0.0",
          "contentHash": "f19f34",
          "fullHash": "879e14f702663dc3a164b63a3516ef855d21353bb9473985f8fb8b3f9f4d5f36"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "Movie",
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "Movie",
      "fullyQualifiedName": "Movie",
      "memberQualifiedName": {
        "fullyQualifiedName": "Movie",
        "parameters": [],
        "name": "Movie",
        "parameterizedName": "Movie",
        "namespace": "",
        "shortDisplayName": "Movie",
        "longDisplayName": "Movie"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "FirstName",
        "parameters": [],
        "name": "FirstName",
        "parameterizedName": "FirstName",
        "namespace": "",
        "shortDisplayName": "FirstName",
        "longDisplayName": "FirstName"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "shortDisplayName": "String",
          "longDisplayName": "lang.taxi.String"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "<unknown>",
          "version": "0.0.0",
          "content": "FirstName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "dummy",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/dummy",
            "id": "com.orbitalhq/dummy/0.1.0",
            "uriSafeId": "com.orbitalhq:dummy:0.1.0"
          },
          "language": "taxi",
          "packageQualifiedName": "[com.orbitalhq/dummy/0.1.0]/<unknown>",
          "id": "<unknown>:0.0.0",
          "contentHash": "9bd55b",
          "fullHash": "14d89f798d0fba8f0769795801d1e4724ba2bbc8853d0ce0b34a351c3ad98506"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "FirstName",
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.String",
        "parameters": [],
        "name": "String",
        "parameterizedName": "lang.taxi.String",
        "namespace": "lang.taxi",
        "shortDisplayName": "String",
        "longDisplayName": "lang.taxi.String"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "FirstName",
      "fullyQualifiedName": "FirstName",
      "memberQualifiedName": {
        "fullyQualifiedName": "FirstName",
        "parameters": [],
        "name": "FirstName",
        "parameterizedName": "FirstName",
        "namespace": "",
        "shortDisplayName": "FirstName",
        "longDisplayName": "FirstName"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "LastName",
        "parameters": [],
        "name": "LastName",
        "parameterizedName": "LastName",
        "namespace": "",
        "shortDisplayName": "LastName",
        "longDisplayName": "LastName"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "name": "String",
          "parameterizedName": "lang.taxi.String",
          "namespace": "lang.taxi",
          "shortDisplayName": "String",
          "longDisplayName": "lang.taxi.String"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "<unknown>",
          "version": "0.0.0",
          "content": "LastName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "dummy",
            "version": "0.1.0",
            "unversionedId": "com.orbitalhq/dummy",
            "id": "com.orbitalhq/dummy/0.1.0",
            "uriSafeId": "com.orbitalhq:dummy:0.1.0"
          },
          "language": "taxi",
          "packageQualifiedName": "[com.orbitalhq/dummy/0.1.0]/<unknown>",
          "id": "<unknown>:0.0.0",
          "contentHash": "5df57e",
          "fullHash": "01457b2e76fc400c4a82ceaaba922c97b256b44442f5394012ae52199343a3b5"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "LastName",
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": false,
        "isEmpty": true
      },
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": true,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.String",
        "parameters": [],
        "name": "String",
        "parameterizedName": "lang.taxi.String",
        "namespace": "lang.taxi",
        "shortDisplayName": "String",
        "longDisplayName": "lang.taxi.String"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "LastName",
      "fullyQualifiedName": "LastName",
      "memberQualifiedName": {
        "fullyQualifiedName": "LastName",
        "parameters": [],
        "name": "LastName",
        "parameterizedName": "LastName",
        "namespace": "",
        "shortDisplayName": "LastName",
        "longDisplayName": "LastName"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    }
  ],
  "compilationErrors": [],
  "parseResult": {
    "raw": {
      "actors": [
        {
          "firstName": "Mel",
          "lastName": "Gibson",
          "agent": {
            "firstName": "Johnny",
            "lastName": "Cashpott"
          }
        },
        {
          "firstName": "Jack",
          "lastName": "Spratt",
          "agent": {
            "firstName": "Johnny",
            "lastName": "Cashpott"
          }
        }
      ],
      "director": {
        "firstName": "Steve",
        "lastName": "Speilburg"
      },
      "title": "Star Wars"
    },
    "typeHints": [
      {
        "start": {
          "line": 2,
          "char": 3
        },
        "startOffset": 4,
        "path": "actors",
        "type": {
          "fullyQualifiedName": "lang.taxi.Array",
          "parameters": [
            {
              "fullyQualifiedName": "Actor",
              "parameters": [],
              "name": "Actor",
              "parameterizedName": "Actor",
              "namespace": "",
              "shortDisplayName": "Actor",
              "longDisplayName": "Actor"
            }
          ],
          "name": "Array",
          "parameterizedName": "lang.taxi.Array<Actor>",
          "namespace": "lang.taxi",
          "shortDisplayName": "Actor[]",
          "longDisplayName": "Actor[]"
        }
      },
      {
        "start": {
          "line": 3,
          "char": 5
        },
        "startOffset": 23,
        "path": "actors.[0].firstName",
        "type": {
          "fullyQualifiedName": "FirstName",
          "parameters": [],
          "name": "FirstName",
          "parameterizedName": "FirstName",
          "namespace": "",
          "shortDisplayName": "FirstName",
          "longDisplayName": "FirstName"
        }
      },
      {
        "start": {
          "line": 4,
          "char": 5
        },
        "startOffset": 48,
        "path": "actors.[0].lastName",
        "type": {
          "fullyQualifiedName": "LastName",
          "parameters": [],
          "name": "LastName",
          "parameterizedName": "LastName",
          "namespace": "",
          "shortDisplayName": "LastName",
          "longDisplayName": "LastName"
        }
      },
      {
        "start": {
          "line": 5,
          "char": 5
        },
        "startOffset": 75,
        "path": "actors.[0].agent",
        "type": {
          "fullyQualifiedName": "Person",
          "parameters": [],
          "name": "Person",
          "parameterizedName": "Person",
          "namespace": "",
          "shortDisplayName": "Person",
          "longDisplayName": "Person"
        }
      },
      {
        "start": {
          "line": 6,
          "char": 7
        },
        "startOffset": 93,
        "path": "actors.[0].agent.firstName",
        "type": {
          "fullyQualifiedName": "FirstName",
          "parameters": [],
          "name": "FirstName",
          "parameterizedName": "FirstName",
          "namespace": "",
          "shortDisplayName": "FirstName",
          "longDisplayName": "FirstName"
        }
      },
      {
        "start": {
          "line": 7,
          "char": 7
        },
        "startOffset": 123,
        "path": "actors.[0].agent.lastName",
        "type": {
          "fullyQualifiedName": "LastName",
          "parameters": [],
          "name": "LastName",
          "parameterizedName": "LastName",
          "namespace": "",
          "shortDisplayName": "LastName",
          "longDisplayName": "LastName"
        }
      },
      {
        "start": {
          "line": 10,
          "char": 5
        },
        "startOffset": 164,
        "path": "actors.[1].firstName",
        "type": {
          "fullyQualifiedName": "FirstName",
          "parameters": [],
          "name": "FirstName",
          "parameterizedName": "FirstName",
          "namespace": "",
          "shortDisplayName": "FirstName",
          "longDisplayName": "FirstName"
        }
      },
      {
        "start": {
          "line": 11,
          "char": 5
        },
        "startOffset": 190,
        "path": "actors.[1].lastName",
        "type": {
          "fullyQualifiedName": "LastName",
          "parameters": [],
          "name": "LastName",
          "parameterizedName": "LastName",
          "namespace": "",
          "shortDisplayName": "LastName",
          "longDisplayName": "LastName"
        }
      },
      {
        "start": {
          "line": 12,
          "char": 5
        },
        "startOffset": 217,
        "path": "actors.[1].agent",
        "type": {
          "fullyQualifiedName": "Person",
          "parameters": [],
          "name": "Person",
          "parameterizedName": "Person",
          "namespace": "",
          "shortDisplayName": "Person",
          "longDisplayName": "Person"
        }
      },
      {
        "start": {
          "line": 13,
          "char": 7
        },
        "startOffset": 235,
        "path": "actors.[1].agent.firstName",
        "type": {
          "fullyQualifiedName": "FirstName",
          "parameters": [],
          "name": "FirstName",
          "parameterizedName": "FirstName",
          "namespace": "",
          "shortDisplayName": "FirstName",
          "longDisplayName": "FirstName"
        }
      },
      {
        "start": {
          "line": 14,
          "char": 7
        },
        "startOffset": 265,
        "path": "actors.[1].agent.lastName",
        "type": {
          "fullyQualifiedName": "LastName",
          "parameters": [],
          "name": "LastName",
          "parameterizedName": "LastName",
          "namespace": "",
          "shortDisplayName": "LastName",
          "longDisplayName": "LastName"
        }
      },
      {
        "start": {
          "line": 17,
          "char": 3
        },
        "startOffset": 304,
        "path": "director",
        "type": {
          "fullyQualifiedName": "Person",
          "parameters": [],
          "name": "Person",
          "parameterizedName": "Person",
          "namespace": "",
          "shortDisplayName": "Person",
          "longDisplayName": "Person"
        }
      },
      {
        "start": {
          "line": 18,
          "char": 5
        },
        "startOffset": 323,
        "path": "director.firstName",
        "type": {
          "fullyQualifiedName": "FirstName",
          "parameters": [],
          "name": "FirstName",
          "parameterizedName": "FirstName",
          "namespace": "",
          "shortDisplayName": "FirstName",
          "longDisplayName": "FirstName"
        }
      },
      {
        "start": {
          "line": 19,
          "char": 5
        },
        "startOffset": 350,
        "path": "director.lastName",
        "type": {
          "fullyQualifiedName": "LastName",
          "parameters": [],
          "name": "LastName",
          "parameterizedName": "LastName",
          "namespace": "",
          "shortDisplayName": "LastName",
          "longDisplayName": "LastName"
        }
      },
      {
        "start": {
          "line": 21,
          "char": 3
        },
        "startOffset": 382,
        "path": "title",
        "type": {
          "fullyQualifiedName": "Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "Title",
          "namespace": "",
          "shortDisplayName": "Title",
          "longDisplayName": "Title"
        }
      }
    ],
    "parseErrors": [],
    "json": "{\n  \"actors\" : [ {\n    \"firstName\" : \"Mel\",\n    \"lastName\" : \"Gibson\",\n    \"agent\" : {\n      \"firstName\" : \"Johnny\",\n      \"lastName\" : \"Cashpott\"\n    }\n  }, {\n    \"firstName\" : \"Jack\",\n    \"lastName\" : \"Spratt\",\n    \"agent\" : {\n      \"firstName\" : \"Johnny\",\n      \"lastName\" : \"Cashpott\"\n    }\n  } ],\n  \"director\" : {\n    \"firstName\" : \"Steve\",\n    \"lastName\" : \"Speilburg\"\n  },\n  \"title\" : \"Star Wars\"\n}"
  },
  "hasParseErrors": false,
  "hasCompilationErrors": false,
  "hasErrors": false
}
