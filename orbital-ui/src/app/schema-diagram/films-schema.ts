import {Schema, VersionType} from '../services/schema';

export const FILMS_SCHEMA = {
  "types": [
    {
      "name": {
        "fullyQualifiedName": "taxi.http.HttpService",
        "parameters": [],
        "name": "HttpService",
        "parameterizedName": "taxi.http.HttpService",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpService",
        "longDisplayName": "taxi.http.HttpService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation HttpService {\n               baseUrl : String\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "83b5c5",
          "fullHash": "1dd416597ff31a9b050dbd783f2d0860dbe8417ddec18b9f9870bd2ef2505f9f"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.HttpService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.HttpService",
      "fullyQualifiedName": "taxi.http.HttpService",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.HttpService",
        "parameters": [],
        "name": "HttpService",
        "parameterizedName": "taxi.http.HttpService",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpService",
        "longDisplayName": "taxi.http.HttpService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.HttpService",
          "parameters": [],
          "name": "HttpService",
          "parameterizedName": "taxi.http.HttpService",
          "namespace": "taxi.http",
          "shortDisplayName": "HttpService",
          "longDisplayName": "taxi.http.HttpService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.HttpMethod",
        "parameters": [],
        "name": "HttpMethod",
        "parameterizedName": "taxi.http.HttpMethod",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpMethod",
        "longDisplayName": "taxi.http.HttpMethod"
      },
      "attributes": {},
      "modifiers": [
        "ENUM"
      ],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [
        {
          "name": "GET",
          "value": "GET",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "POST",
          "value": "POST",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "PUT",
          "value": "PUT",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "DELETE",
          "value": "DELETE",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "PATCH",
          "value": "PATCH",
          "synonyms": [],
          "typeDoc": ""
        }
      ],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "enum HttpMethod {\n               GET,\n               POST,\n               PUT,\n               DELETE,\n               PATCH\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "86af4e",
          "fullHash": "afd63dba8f7f5c342f37d3c925315e8ffce5e9645772535a8e6bc337862b7506"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.HttpMethod",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "declaresFormat": false,
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
      "longDisplayName": "taxi.http.HttpMethod",
      "fullyQualifiedName": "taxi.http.HttpMethod",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.HttpMethod",
        "parameters": [],
        "name": "HttpMethod",
        "parameterizedName": "taxi.http.HttpMethod",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpMethod",
        "longDisplayName": "taxi.http.HttpMethod"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.HttpMethod",
          "parameters": [],
          "name": "HttpMethod",
          "parameterizedName": "taxi.http.HttpMethod",
          "namespace": "taxi.http",
          "shortDisplayName": "HttpMethod",
          "longDisplayName": "taxi.http.HttpMethod"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.HttpOperation",
        "parameters": [],
        "name": "HttpOperation",
        "parameterizedName": "taxi.http.HttpOperation",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpOperation",
        "longDisplayName": "taxi.http.HttpOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation HttpOperation {\n               method : HttpMethod\n               url : String\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "aa494d",
          "fullHash": "98246a47985107c3a74c6fa66bb399988afa4db9670f086f60ca269f38d42d74"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.HttpOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.HttpOperation",
      "fullyQualifiedName": "taxi.http.HttpOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.HttpOperation",
        "parameters": [],
        "name": "HttpOperation",
        "parameterizedName": "taxi.http.HttpOperation",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpOperation",
        "longDisplayName": "taxi.http.HttpOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.HttpOperation",
          "parameters": [],
          "name": "HttpOperation",
          "parameterizedName": "taxi.http.HttpOperation",
          "namespace": "taxi.http",
          "shortDisplayName": "HttpOperation",
          "longDisplayName": "taxi.http.HttpOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.WebsocketOperation",
        "parameters": [],
        "name": "WebsocketOperation",
        "parameterizedName": "taxi.http.WebsocketOperation",
        "namespace": "taxi.http",
        "shortDisplayName": "WebsocketOperation",
        "longDisplayName": "taxi.http.WebsocketOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation WebsocketOperation {\n               path : String\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "18cb78",
          "fullHash": "0100da7282f7ab4c796c6ac61101a03dfae4ce174c498609eaf147073c902a5f"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.WebsocketOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.WebsocketOperation",
      "fullyQualifiedName": "taxi.http.WebsocketOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.WebsocketOperation",
        "parameters": [],
        "name": "WebsocketOperation",
        "parameterizedName": "taxi.http.WebsocketOperation",
        "namespace": "taxi.http",
        "shortDisplayName": "WebsocketOperation",
        "longDisplayName": "taxi.http.WebsocketOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.WebsocketOperation",
          "parameters": [],
          "name": "WebsocketOperation",
          "parameterizedName": "taxi.http.WebsocketOperation",
          "namespace": "taxi.http",
          "shortDisplayName": "WebsocketOperation",
          "longDisplayName": "taxi.http.WebsocketOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.HttpHeader",
        "parameters": [],
        "name": "HttpHeader",
        "parameterizedName": "taxi.http.HttpHeader",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpHeader",
        "longDisplayName": "taxi.http.HttpHeader"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation HttpHeader {\n               name : String\n               [[ Pass a value when using as an annotation on an operation.\n               For parameters, it's valid to allow the value to be populated from the parameter. ]]\n               value : String?\n               prefix : String?\n               suffix : String?\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "c9301d",
          "fullHash": "4b94437e162e65ec568ab6d115786e0d3144153def217373ee89b470547b4fd9"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.HttpHeader",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.HttpHeader",
      "fullyQualifiedName": "taxi.http.HttpHeader",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.HttpHeader",
        "parameters": [],
        "name": "HttpHeader",
        "parameterizedName": "taxi.http.HttpHeader",
        "namespace": "taxi.http",
        "shortDisplayName": "HttpHeader",
        "longDisplayName": "taxi.http.HttpHeader"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.HttpHeader",
          "parameters": [],
          "name": "HttpHeader",
          "parameterizedName": "taxi.http.HttpHeader",
          "namespace": "taxi.http",
          "shortDisplayName": "HttpHeader",
          "longDisplayName": "taxi.http.HttpHeader"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.RequestBody",
        "parameters": [],
        "name": "RequestBody",
        "parameterizedName": "taxi.http.RequestBody",
        "namespace": "taxi.http",
        "shortDisplayName": "RequestBody",
        "longDisplayName": "taxi.http.RequestBody"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation RequestBody {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "cddd44",
          "fullHash": "5176f4dffbee59e4508b2b620fc7fa3a50dbe3735d7ada89fa4f7335f4570a91"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.RequestBody",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.RequestBody",
      "fullyQualifiedName": "taxi.http.RequestBody",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.RequestBody",
        "parameters": [],
        "name": "RequestBody",
        "parameterizedName": "taxi.http.RequestBody",
        "namespace": "taxi.http",
        "shortDisplayName": "RequestBody",
        "longDisplayName": "taxi.http.RequestBody"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.RequestBody",
          "parameters": [],
          "name": "RequestBody",
          "parameterizedName": "taxi.http.RequestBody",
          "namespace": "taxi.http",
          "shortDisplayName": "RequestBody",
          "longDisplayName": "taxi.http.RequestBody"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.PathVariable",
        "parameters": [],
        "name": "PathVariable",
        "parameterizedName": "taxi.http.PathVariable",
        "namespace": "taxi.http",
        "shortDisplayName": "PathVariable",
        "longDisplayName": "taxi.http.PathVariable"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation PathVariable { value : String }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "63aa5c",
          "fullHash": "4510fc798cea1b2b074f957f4ba91aa5e505a2f3d5505565952c4a359ce23f39"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.PathVariable",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.PathVariable",
      "fullyQualifiedName": "taxi.http.PathVariable",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.PathVariable",
        "parameters": [],
        "name": "PathVariable",
        "parameterizedName": "taxi.http.PathVariable",
        "namespace": "taxi.http",
        "shortDisplayName": "PathVariable",
        "longDisplayName": "taxi.http.PathVariable"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.PathVariable",
          "parameters": [],
          "name": "PathVariable",
          "parameterizedName": "taxi.http.PathVariable",
          "namespace": "taxi.http",
          "shortDisplayName": "PathVariable",
          "longDisplayName": "taxi.http.PathVariable"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.ResponseBody",
        "parameters": [],
        "name": "ResponseBody",
        "parameterizedName": "taxi.http.ResponseBody",
        "namespace": "taxi.http",
        "shortDisplayName": "ResponseBody",
        "longDisplayName": "taxi.http.ResponseBody"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation ResponseBody{}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "09541c",
          "fullHash": "dbe90f12c04e3269e5e43c5c5eaf6092a2b5ffc75c931856645dad31f52881ef"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.ResponseBody",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.ResponseBody",
      "fullyQualifiedName": "taxi.http.ResponseBody",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.ResponseBody",
        "parameters": [],
        "name": "ResponseBody",
        "parameterizedName": "taxi.http.ResponseBody",
        "namespace": "taxi.http",
        "shortDisplayName": "ResponseBody",
        "longDisplayName": "taxi.http.ResponseBody"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.ResponseBody",
          "parameters": [],
          "name": "ResponseBody",
          "parameterizedName": "taxi.http.ResponseBody",
          "namespace": "taxi.http",
          "shortDisplayName": "ResponseBody",
          "longDisplayName": "taxi.http.ResponseBody"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "taxi.http.ResponseCode",
        "parameters": [],
        "name": "ResponseCode",
        "parameterizedName": "taxi.http.ResponseCode",
        "namespace": "taxi.http",
        "shortDisplayName": "ResponseCode",
        "longDisplayName": "taxi.http.ResponseCode"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "version": "0.0.0",
          "content": "annotation ResponseCode {\n               value : Int\n            }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/taxi.http",
          "id": "[com.orbitalhq/core-types/1.0.0]/taxi.http:0.0.0",
          "contentHash": "9454e0",
          "fullHash": "7055b59f673367cff5f109c8ddc311bab49cbc72f7c80b56c28cfb6601ea3732"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "taxi.http.ResponseCode",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "taxi.http.ResponseCode",
      "fullyQualifiedName": "taxi.http.ResponseCode",
      "memberQualifiedName": {
        "fullyQualifiedName": "taxi.http.ResponseCode",
        "parameters": [],
        "name": "ResponseCode",
        "parameterizedName": "taxi.http.ResponseCode",
        "namespace": "taxi.http",
        "shortDisplayName": "ResponseCode",
        "longDisplayName": "taxi.http.ResponseCode"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "taxi.http.ResponseCode",
          "parameters": [],
          "name": "ResponseCode",
          "parameterizedName": "taxi.http.ResponseCode",
          "namespace": "taxi.http",
          "shortDisplayName": "ResponseCode",
          "longDisplayName": "taxi.http.ResponseCode"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.Username",
        "parameters": [],
        "name": "Username",
        "parameterizedName": "com.orbitalhq.Username",
        "namespace": "com.orbitalhq",
        "shortDisplayName": "Username",
        "longDisplayName": "com.orbitalhq.Username"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/UserTypes",
          "version": "0.0.0",
          "content": "type Username inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/UserTypes",
          "id": "[com.orbitalhq/core-types/1.0.0]/UserTypes:0.0.0",
          "contentHash": "de78f4",
          "fullHash": "6913e405823029ab47cccd9978ce362157dd69c179ccfb0cdd7dbd750fb736d3"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.Username",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.Username",
      "fullyQualifiedName": "com.orbitalhq.Username",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.Username",
        "parameters": [],
        "name": "Username",
        "parameterizedName": "com.orbitalhq.Username",
        "namespace": "com.orbitalhq",
        "shortDisplayName": "Username",
        "longDisplayName": "com.orbitalhq.Username"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.Username",
          "parameters": [],
          "name": "Username",
          "parameterizedName": "com.orbitalhq.Username",
          "namespace": "com.orbitalhq",
          "shortDisplayName": "Username",
          "longDisplayName": "com.orbitalhq.Username"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.auth.AuthClaims",
        "parameters": [],
        "name": "AuthClaims",
        "parameterizedName": "com.orbitalhq.auth.AuthClaims",
        "namespace": "com.orbitalhq.auth",
        "shortDisplayName": "AuthClaims",
        "longDisplayName": "com.orbitalhq.auth.AuthClaims"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JwtTypes",
          "version": "0.0.0",
          "content": "model AuthClaims {\n\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JwtTypes",
          "id": "[com.orbitalhq/core-types/1.0.0]/JwtTypes:0.0.0",
          "contentHash": "a70956",
          "fullHash": "d415487bf1a38df27b2cac6789a55feb7a39f7a8876ce7c34480a817e5fbba30"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.auth.AuthClaims",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.auth.AuthClaims",
      "fullyQualifiedName": "com.orbitalhq.auth.AuthClaims",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.auth.AuthClaims",
        "parameters": [],
        "name": "AuthClaims",
        "parameterizedName": "com.orbitalhq.auth.AuthClaims",
        "namespace": "com.orbitalhq.auth",
        "shortDisplayName": "AuthClaims",
        "longDisplayName": "com.orbitalhq.auth.AuthClaims"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.auth.AuthClaims",
          "parameters": [],
          "name": "AuthClaims",
          "parameterizedName": "com.orbitalhq.auth.AuthClaims",
          "namespace": "com.orbitalhq.auth",
          "shortDisplayName": "AuthClaims",
          "longDisplayName": "com.orbitalhq.auth.AuthClaims"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.errors.ErrorMessage",
        "parameters": [],
        "name": "ErrorMessage",
        "parameterizedName": "com.orbitalhq.errors.ErrorMessage",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "ErrorMessage",
        "longDisplayName": "com.orbitalhq.errors.ErrorMessage"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "version": "0.0.0",
          "content": "type ErrorMessage inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "id": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError:0.0.0",
          "contentHash": "d47a66",
          "fullHash": "a1ce6b2ad6980b8df0a7815307b8557c370b830ec41174d62af287533789703b"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.errors.ErrorMessage",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.errors.ErrorMessage",
      "fullyQualifiedName": "com.orbitalhq.errors.ErrorMessage",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.errors.ErrorMessage",
        "parameters": [],
        "name": "ErrorMessage",
        "parameterizedName": "com.orbitalhq.errors.ErrorMessage",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "ErrorMessage",
        "longDisplayName": "com.orbitalhq.errors.ErrorMessage"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.errors.ErrorMessage",
          "parameters": [],
          "name": "ErrorMessage",
          "parameterizedName": "com.orbitalhq.errors.ErrorMessage",
          "namespace": "com.orbitalhq.errors",
          "shortDisplayName": "ErrorMessage",
          "longDisplayName": "com.orbitalhq.errors.ErrorMessage"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.errors.Error",
        "parameters": [],
        "name": "Error",
        "parameterizedName": "com.orbitalhq.errors.Error",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "Error",
        "longDisplayName": "com.orbitalhq.errors.Error"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "version": "0.0.0",
          "content": "model Error {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "id": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError:0.0.0",
          "contentHash": "b465f7",
          "fullHash": "0e7de3219b2ba002be12d2cc2c4b1542c5e42a4fb83d00e1141af6628dad61ef"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.errors.Error",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.errors.Error",
      "fullyQualifiedName": "com.orbitalhq.errors.Error",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.errors.Error",
        "parameters": [],
        "name": "Error",
        "parameterizedName": "com.orbitalhq.errors.Error",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "Error",
        "longDisplayName": "com.orbitalhq.errors.Error"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.errors.Error",
          "parameters": [],
          "name": "Error",
          "parameterizedName": "com.orbitalhq.errors.Error",
          "namespace": "com.orbitalhq.errors",
          "shortDisplayName": "Error",
          "longDisplayName": "com.orbitalhq.errors.Error"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.errors.NotAuthorizedError",
        "parameters": [],
        "name": "NotAuthorizedError",
        "parameterizedName": "com.orbitalhq.errors.NotAuthorizedError",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "NotAuthorizedError",
        "longDisplayName": "com.orbitalhq.errors.NotAuthorizedError"
      },
      "attributes": {
        "message": {
          "type": {
            "fullyQualifiedName": "com.orbitalhq.errors.ErrorMessage",
            "parameters": [],
            "name": "ErrorMessage",
            "parameterizedName": "com.orbitalhq.errors.ErrorMessage",
            "namespace": "com.orbitalhq.errors",
            "shortDisplayName": "ErrorMessage",
            "longDisplayName": "com.orbitalhq.errors.ErrorMessage"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "com.orbitalhq.errors.ErrorMessage",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        }
      },
      "modifiers": [],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "taxi.http.ResponseCode",
            "parameters": [],
            "name": "ResponseCode",
            "parameterizedName": "taxi.http.ResponseCode",
            "namespace": "taxi.http",
            "shortDisplayName": "ResponseCode",
            "longDisplayName": "taxi.http.ResponseCode"
          },
          "params": {
            "value": 401
          }
        }
      ],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "com.orbitalhq.errors.Error",
          "parameters": [],
          "name": "Error",
          "parameterizedName": "com.orbitalhq.errors.Error",
          "namespace": "com.orbitalhq.errors",
          "shortDisplayName": "Error",
          "longDisplayName": "com.orbitalhq.errors.Error"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "version": "0.0.0",
          "content": "@taxi.http.ResponseCode(401)\n   model NotAuthorizedError inherits Error {\n     message: ErrorMessage\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError",
          "id": "[com.orbitalhq/core-types/1.0.0]/VyneQueryError:0.0.0",
          "contentHash": "f6d756",
          "fullHash": "4a22c92c286ede06b2fa523f703f2cd945b8acb5f4cd401b1e05cd245be09b00"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.errors.NotAuthorizedError",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.errors.NotAuthorizedError",
      "fullyQualifiedName": "com.orbitalhq.errors.NotAuthorizedError",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.errors.NotAuthorizedError",
        "parameters": [],
        "name": "NotAuthorizedError",
        "parameterizedName": "com.orbitalhq.errors.NotAuthorizedError",
        "namespace": "com.orbitalhq.errors",
        "shortDisplayName": "NotAuthorizedError",
        "longDisplayName": "com.orbitalhq.errors.NotAuthorizedError"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.errors.NotAuthorizedError",
          "parameters": [],
          "name": "NotAuthorizedError",
          "parameterizedName": "com.orbitalhq.errors.NotAuthorizedError",
          "namespace": "com.orbitalhq.errors",
          "shortDisplayName": "NotAuthorizedError",
          "longDisplayName": "com.orbitalhq.errors.NotAuthorizedError"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.jdbc.ConnectionName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.jdbc.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "type ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "716d89",
          "fullHash": "7629d696dc229f5088caa3b50b8d076a53c6538d7ee020303dddee07dfbb7340"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.jdbc.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.jdbc.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.jdbc.ConnectionName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.jdbc.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.jdbc.ConnectionName",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.jdbc.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.jdbc.UpsertOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.jdbc.UpsertOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "annotation UpsertOperation {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "970f39",
          "fullHash": "5a52c257b92d57af2fa36cd4a18f9279a30e329066ffc107ae34b9f084cc1df4"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.UpsertOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.jdbc.UpsertOperation",
      "fullyQualifiedName": "com.orbitalhq.jdbc.UpsertOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.jdbc.UpsertOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.jdbc.UpsertOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.UpsertOperation",
          "parameters": [],
          "name": "UpsertOperation",
          "parameterizedName": "com.orbitalhq.jdbc.UpsertOperation",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "UpsertOperation",
          "longDisplayName": "com.orbitalhq.jdbc.UpsertOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.InsertOperation",
        "parameters": [],
        "name": "InsertOperation",
        "parameterizedName": "com.orbitalhq.jdbc.InsertOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "InsertOperation",
        "longDisplayName": "com.orbitalhq.jdbc.InsertOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "annotation InsertOperation {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "4d763e",
          "fullHash": "4b311825b2c64c082fd160ceabe90917b5be6d0f991e19504b199206a0910a83"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.InsertOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.jdbc.InsertOperation",
      "fullyQualifiedName": "com.orbitalhq.jdbc.InsertOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.InsertOperation",
        "parameters": [],
        "name": "InsertOperation",
        "parameterizedName": "com.orbitalhq.jdbc.InsertOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "InsertOperation",
        "longDisplayName": "com.orbitalhq.jdbc.InsertOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.InsertOperation",
          "parameters": [],
          "name": "InsertOperation",
          "parameterizedName": "com.orbitalhq.jdbc.InsertOperation",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "InsertOperation",
          "longDisplayName": "com.orbitalhq.jdbc.InsertOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.UpdateOperation",
        "parameters": [],
        "name": "UpdateOperation",
        "parameterizedName": "com.orbitalhq.jdbc.UpdateOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "UpdateOperation",
        "longDisplayName": "com.orbitalhq.jdbc.UpdateOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "annotation UpdateOperation {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "ceb444",
          "fullHash": "c9ed8bee04a1893713e977cd4a70882d69b675f4d403a662aa3e99ab679d564c"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.UpdateOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.jdbc.UpdateOperation",
      "fullyQualifiedName": "com.orbitalhq.jdbc.UpdateOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.UpdateOperation",
        "parameters": [],
        "name": "UpdateOperation",
        "parameterizedName": "com.orbitalhq.jdbc.UpdateOperation",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "UpdateOperation",
        "longDisplayName": "com.orbitalhq.jdbc.UpdateOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.UpdateOperation",
          "parameters": [],
          "name": "UpdateOperation",
          "parameterizedName": "com.orbitalhq.jdbc.UpdateOperation",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "UpdateOperation",
          "longDisplayName": "com.orbitalhq.jdbc.UpdateOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.DatabaseService",
        "parameters": [],
        "name": "DatabaseService",
        "parameterizedName": "com.orbitalhq.jdbc.DatabaseService",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "DatabaseService",
        "longDisplayName": "com.orbitalhq.jdbc.DatabaseService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "annotation DatabaseService {\n      connection : ConnectionName\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "6e283c",
          "fullHash": "ecdaebaa9f82a321480ebbef58f6f6576c7b3e83df09d1305df28ff519a563b4"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.DatabaseService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.jdbc.DatabaseService",
      "fullyQualifiedName": "com.orbitalhq.jdbc.DatabaseService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.DatabaseService",
        "parameters": [],
        "name": "DatabaseService",
        "parameterizedName": "com.orbitalhq.jdbc.DatabaseService",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "DatabaseService",
        "longDisplayName": "com.orbitalhq.jdbc.DatabaseService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.DatabaseService",
          "parameters": [],
          "name": "DatabaseService",
          "parameterizedName": "com.orbitalhq.jdbc.DatabaseService",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "DatabaseService",
          "longDisplayName": "com.orbitalhq.jdbc.DatabaseService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.Table",
        "parameters": [],
        "name": "Table",
        "parameterizedName": "com.orbitalhq.jdbc.Table",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "Table",
        "longDisplayName": "com.orbitalhq.jdbc.Table"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "annotation Table {\n      connection : ConnectionName\n      table : TableName inherits String\n      schema: SchemaName inherits String\n\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "0a8b74",
          "fullHash": "8cc968acc3ade9074329fbb19aeac8167c8627e41d7b54c1f71e73e1dc7b8566"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.Table",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.jdbc.Table",
      "fullyQualifiedName": "com.orbitalhq.jdbc.Table",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.Table",
        "parameters": [],
        "name": "Table",
        "parameterizedName": "com.orbitalhq.jdbc.Table",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "Table",
        "longDisplayName": "com.orbitalhq.jdbc.Table"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.Table",
          "parameters": [],
          "name": "Table",
          "parameterizedName": "com.orbitalhq.jdbc.Table",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "Table",
          "longDisplayName": "com.orbitalhq.jdbc.Table"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageKey",
        "parameters": [],
        "name": "KafkaMessageKey",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMessageKey",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMessageKey",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMessageKey"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "version": "0.0.0",
          "content": "annotation KafkaMessageKey {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors:0.0.0",
          "contentHash": "8c86b3",
          "fullHash": "f7765fc39a86c87ae6d6415cb6062946e59ed2a6ced6a8a6f905055c61f86d16"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaMessageKey",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.kafka.KafkaMessageKey",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageKey",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageKey",
        "parameters": [],
        "name": "KafkaMessageKey",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMessageKey",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMessageKey",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMessageKey"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageKey",
          "parameters": [],
          "name": "KafkaMessageKey",
          "parameterizedName": "com.orbitalhq.kafka.KafkaMessageKey",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaMessageKey",
          "longDisplayName": "com.orbitalhq.kafka.KafkaMessageKey"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
        "parameters": [],
        "name": "KafkaMessageMetadata",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMessageMetadata",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMessageMetadata"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "version": "0.0.0",
          "content": "annotation KafkaMessageMetadata {\n      value : KafkaMetadataType\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors:0.0.0",
          "contentHash": "4eb147",
          "fullHash": "bf76128cc221ff7e59557ca170b878d8c1f39346da1d5484bd4893b77421a5b2"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.kafka.KafkaMessageMetadata",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
        "parameters": [],
        "name": "KafkaMessageMetadata",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMessageMetadata",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMessageMetadata"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
          "parameters": [],
          "name": "KafkaMessageMetadata",
          "parameterizedName": "com.orbitalhq.kafka.KafkaMessageMetadata",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaMessageMetadata",
          "longDisplayName": "com.orbitalhq.kafka.KafkaMessageMetadata"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMetadataType",
        "parameters": [],
        "name": "KafkaMetadataType",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMetadataType",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMetadataType",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMetadataType"
      },
      "attributes": {},
      "modifiers": [
        "ENUM"
      ],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [
        {
          "name": "Partition",
          "value": "Partition",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "Offset",
          "value": "Offset",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "Timestamp",
          "value": "Timestamp",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "TimestampType",
          "value": "TimestampType",
          "synonyms": [],
          "typeDoc": ""
        }
      ],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "version": "0.0.0",
          "content": "enum KafkaMetadataType {\n      Partition,\n      Offset,\n      Timestamp,\n      TimestampType\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors:0.0.0",
          "contentHash": "fb21c9",
          "fullHash": "13dda9197bd68cebd0b40df983608738124c5848af25e9a5919d71fa0ee8de3f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaMetadataType",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "declaresFormat": false,
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
      "longDisplayName": "com.orbitalhq.kafka.KafkaMetadataType",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMetadataType",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMetadataType",
        "parameters": [],
        "name": "KafkaMetadataType",
        "parameterizedName": "com.orbitalhq.kafka.KafkaMetadataType",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaMetadataType",
        "longDisplayName": "com.orbitalhq.kafka.KafkaMetadataType"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaMetadataType",
          "parameters": [],
          "name": "KafkaMetadataType",
          "parameterizedName": "com.orbitalhq.kafka.KafkaMetadataType",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaMetadataType",
          "longDisplayName": "com.orbitalhq.kafka.KafkaMetadataType"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaHeader",
        "parameters": [],
        "name": "KafkaHeader",
        "parameterizedName": "com.orbitalhq.kafka.KafkaHeader",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaHeader",
        "longDisplayName": "com.orbitalhq.kafka.KafkaHeader"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "version": "0.0.0",
          "content": "annotation KafkaHeader {\n      value : String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/KafkaConnectors:0.0.0",
          "contentHash": "b02a5a",
          "fullHash": "d2c566b4e8c35afd5f7e1cb286ef97ebf3ab51555c0eb80b279ce134d9c38f5d"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaHeader",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.kafka.KafkaHeader",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaHeader",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaHeader",
        "parameters": [],
        "name": "KafkaHeader",
        "parameterizedName": "com.orbitalhq.kafka.KafkaHeader",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaHeader",
        "longDisplayName": "com.orbitalhq.kafka.KafkaHeader"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaHeader",
          "parameters": [],
          "name": "KafkaHeader",
          "parameterizedName": "com.orbitalhq.kafka.KafkaHeader",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaHeader",
          "longDisplayName": "com.orbitalhq.kafka.KafkaHeader"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.catalog.DataOwner",
        "parameters": [],
        "name": "DataOwner",
        "parameterizedName": "com.orbitalhq.catalog.DataOwner",
        "namespace": "com.orbitalhq.catalog",
        "shortDisplayName": "DataOwner",
        "longDisplayName": "com.orbitalhq.catalog.DataOwner"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/Catalog",
          "version": "0.0.0",
          "content": "annotation DataOwner {\n      id : com.orbitalhq.Username\n      name : String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/Catalog",
          "id": "[com.orbitalhq/core-types/1.0.0]/Catalog:0.0.0",
          "contentHash": "779de4",
          "fullHash": "9a67ef8fd3d2c5fe033ea27b0df713fe2f907e1c8716d4fcc248a6b823870955"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.catalog.DataOwner",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.catalog.DataOwner",
      "fullyQualifiedName": "com.orbitalhq.catalog.DataOwner",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.catalog.DataOwner",
        "parameters": [],
        "name": "DataOwner",
        "parameterizedName": "com.orbitalhq.catalog.DataOwner",
        "namespace": "com.orbitalhq.catalog",
        "shortDisplayName": "DataOwner",
        "longDisplayName": "com.orbitalhq.catalog.DataOwner"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.catalog.DataOwner",
          "parameters": [],
          "name": "DataOwner",
          "parameterizedName": "com.orbitalhq.catalog.DataOwner",
          "namespace": "com.orbitalhq.catalog",
          "shortDisplayName": "DataOwner",
          "longDisplayName": "com.orbitalhq.catalog.DataOwner"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Service",
        "parameters": [],
        "name": "S3Service",
        "parameterizedName": "com.orbitalhq.aws.s3.S3Service",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3Service",
        "longDisplayName": "com.orbitalhq.aws.s3.S3Service"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "annotation S3Service {\n      connectionName : ConnectionName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "aea2c9",
          "fullHash": "77f6225ac6cb025283d3e00e25c97acda04eb30c038e888f34445bfc1116b4dc"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.S3Service",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.s3.S3Service",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Service",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Service",
        "parameters": [],
        "name": "S3Service",
        "parameterizedName": "com.orbitalhq.aws.s3.S3Service",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3Service",
        "longDisplayName": "com.orbitalhq.aws.s3.S3Service"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Service",
          "parameters": [],
          "name": "S3Service",
          "parameterizedName": "com.orbitalhq.aws.s3.S3Service",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "S3Service",
          "longDisplayName": "com.orbitalhq.aws.s3.S3Service"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.RequestBody",
        "parameters": [],
        "name": "RequestBody",
        "parameterizedName": "com.orbitalhq.aws.s3.RequestBody",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "RequestBody",
        "longDisplayName": "com.orbitalhq.aws.s3.RequestBody"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "annotation RequestBody {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "cddd44",
          "fullHash": "d158100dd0f39b040bb73a388e4d6e6f8a5cbacf1605eb7c8782a33860398540"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.RequestBody",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.s3.RequestBody",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.RequestBody",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.RequestBody",
        "parameters": [],
        "name": "RequestBody",
        "parameterizedName": "com.orbitalhq.aws.s3.RequestBody",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "RequestBody",
        "longDisplayName": "com.orbitalhq.aws.s3.RequestBody"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.RequestBody",
          "parameters": [],
          "name": "RequestBody",
          "parameterizedName": "com.orbitalhq.aws.s3.RequestBody",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "RequestBody",
          "longDisplayName": "com.orbitalhq.aws.s3.RequestBody"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Operation",
        "parameters": [],
        "name": "S3Operation",
        "parameterizedName": "com.orbitalhq.aws.s3.S3Operation",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3Operation",
        "longDisplayName": "com.orbitalhq.aws.s3.S3Operation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "annotation S3Operation {\n      bucket : BucketName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "ae8c1b",
          "fullHash": "16f8f911215017f1b3e2de66515579073cd1aa01e09b37b31a24ea3f7be06973"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.S3Operation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.s3.S3Operation",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Operation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Operation",
        "parameters": [],
        "name": "S3Operation",
        "parameterizedName": "com.orbitalhq.aws.s3.S3Operation",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3Operation",
        "longDisplayName": "com.orbitalhq.aws.s3.S3Operation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.S3Operation",
          "parameters": [],
          "name": "S3Operation",
          "parameterizedName": "com.orbitalhq.aws.s3.S3Operation",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "S3Operation",
          "longDisplayName": "com.orbitalhq.aws.s3.S3Operation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3EntryKey",
        "parameters": [],
        "name": "S3EntryKey",
        "parameterizedName": "com.orbitalhq.aws.s3.S3EntryKey",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3EntryKey",
        "longDisplayName": "com.orbitalhq.aws.s3.S3EntryKey"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "type S3EntryKey inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "58ab3f",
          "fullHash": "895009da31313e6eb0d1305ec413fdb7e0fcdfde2e7cb8cf7ffaf804ba6f05fd"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.S3EntryKey",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.s3.S3EntryKey",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.S3EntryKey",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.S3EntryKey",
        "parameters": [],
        "name": "S3EntryKey",
        "parameterizedName": "com.orbitalhq.aws.s3.S3EntryKey",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "S3EntryKey",
        "longDisplayName": "com.orbitalhq.aws.s3.S3EntryKey"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.S3EntryKey",
          "parameters": [],
          "name": "S3EntryKey",
          "parameterizedName": "com.orbitalhq.aws.s3.S3EntryKey",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "S3EntryKey",
          "longDisplayName": "com.orbitalhq.aws.s3.S3EntryKey"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.FilenamePattern",
        "parameters": [],
        "name": "FilenamePattern",
        "parameterizedName": "com.orbitalhq.aws.s3.FilenamePattern",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "FilenamePattern",
        "longDisplayName": "com.orbitalhq.aws.s3.FilenamePattern"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "[[ A pattern (using standard Glob format) that defines\n   the file(s) to read from S3. Eg:\n    - `foo*.txt`\n    - `*.txt`\n    - `*`\n   ]]\n   type FilenamePattern inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "f9405d",
          "fullHash": "db8fc4610e8d503f08796bd6f5cc5baaac52edd44d670ab4a810dde692cdfc9d"
        }
      ],
      "typeParameters": [],
      "typeDoc": "A pattern (using standard Glob format) that defines\n  the file(s) to read from S3. Eg:\n   - `foo*.txt`\n   - `*.txt`\n   - `*`",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.FilenamePattern",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.s3.FilenamePattern",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.FilenamePattern",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.FilenamePattern",
        "parameters": [],
        "name": "FilenamePattern",
        "parameterizedName": "com.orbitalhq.aws.s3.FilenamePattern",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "FilenamePattern",
        "longDisplayName": "com.orbitalhq.aws.s3.FilenamePattern"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.FilenamePattern",
          "parameters": [],
          "name": "FilenamePattern",
          "parameterizedName": "com.orbitalhq.aws.s3.FilenamePattern",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "FilenamePattern",
          "longDisplayName": "com.orbitalhq.aws.s3.FilenamePattern"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsService",
        "parameters": [],
        "name": "SqsService",
        "parameterizedName": "com.orbitalhq.aws.sqs.SqsService",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "SqsService",
        "longDisplayName": "com.orbitalhq.aws.sqs.SqsService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "version": "0.0.0",
          "content": "annotation SqsService {\n      connectionName : ConnectionName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors:0.0.0",
          "contentHash": "a771ba",
          "fullHash": "08ad6add994970ae7e5464b00d71ab58509f322f900ae03843e25d75ca0477a1"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.sqs.SqsService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.sqs.SqsService",
      "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsService",
        "parameters": [],
        "name": "SqsService",
        "parameterizedName": "com.orbitalhq.aws.sqs.SqsService",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "SqsService",
        "longDisplayName": "com.orbitalhq.aws.sqs.SqsService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsService",
          "parameters": [],
          "name": "SqsService",
          "parameterizedName": "com.orbitalhq.aws.sqs.SqsService",
          "namespace": "com.orbitalhq.aws.sqs",
          "shortDisplayName": "SqsService",
          "longDisplayName": "com.orbitalhq.aws.sqs.SqsService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsOperation",
        "parameters": [],
        "name": "SqsOperation",
        "parameterizedName": "com.orbitalhq.aws.sqs.SqsOperation",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "SqsOperation",
        "longDisplayName": "com.orbitalhq.aws.sqs.SqsOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "version": "0.0.0",
          "content": "annotation SqsOperation {\n      queue : QueueName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors:0.0.0",
          "contentHash": "f17d25",
          "fullHash": "d1a63ced1999b38285e11d11b087cf373f96bf9601e7d3b77a8899b958ee5651"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.sqs.SqsOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.sqs.SqsOperation",
      "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsOperation",
        "parameters": [],
        "name": "SqsOperation",
        "parameterizedName": "com.orbitalhq.aws.sqs.SqsOperation",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "SqsOperation",
        "longDisplayName": "com.orbitalhq.aws.sqs.SqsOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.sqs.SqsOperation",
          "parameters": [],
          "name": "SqsOperation",
          "parameterizedName": "com.orbitalhq.aws.sqs.SqsOperation",
          "namespace": "com.orbitalhq.aws.sqs",
          "shortDisplayName": "SqsOperation",
          "longDisplayName": "com.orbitalhq.aws.sqs.SqsOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.BlobService",
        "parameters": [],
        "name": "BlobService",
        "parameterizedName": "com.orbitalhq.azure.store.BlobService",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "BlobService",
        "longDisplayName": "com.orbitalhq.azure.store.BlobService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "version": "0.0.0",
          "content": "annotation BlobService {\n      connectionName : ConnectionName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors:0.0.0",
          "contentHash": "b491f2",
          "fullHash": "46fbcb8bf9308e2c28890e266b5ce7d68de7d11eb84e1b8b43b7edf7e92906af"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.azure.store.BlobService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.azure.store.BlobService",
      "fullyQualifiedName": "com.orbitalhq.azure.store.BlobService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.BlobService",
        "parameters": [],
        "name": "BlobService",
        "parameterizedName": "com.orbitalhq.azure.store.BlobService",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "BlobService",
        "longDisplayName": "com.orbitalhq.azure.store.BlobService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.azure.store.BlobService",
          "parameters": [],
          "name": "BlobService",
          "parameterizedName": "com.orbitalhq.azure.store.BlobService",
          "namespace": "com.orbitalhq.azure.store",
          "shortDisplayName": "BlobService",
          "longDisplayName": "com.orbitalhq.azure.store.BlobService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreOperation",
        "parameters": [],
        "name": "AzureStoreOperation",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreOperation",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreOperation",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "version": "0.0.0",
          "content": "annotation AzureStoreOperation {\n      container : AzureStoreContainer inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors:0.0.0",
          "contentHash": "eeb9d1",
          "fullHash": "7d0d37eae88a309e2cab7556c7fb855ee6d80623b604edc3bc7567ee8adc12c8"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.azure.store.AzureStoreOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.azure.store.AzureStoreOperation",
      "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreOperation",
        "parameters": [],
        "name": "AzureStoreOperation",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreOperation",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreOperation",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreOperation",
          "parameters": [],
          "name": "AzureStoreOperation",
          "parameterizedName": "com.orbitalhq.azure.store.AzureStoreOperation",
          "namespace": "com.orbitalhq.azure.store",
          "shortDisplayName": "AzureStoreOperation",
          "longDisplayName": "com.orbitalhq.azure.store.AzureStoreOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreBlob",
        "parameters": [],
        "name": "AzureStoreBlob",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreBlob",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreBlob",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreBlob"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "version": "0.0.0",
          "content": "type AzureStoreBlob inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors:0.0.0",
          "contentHash": "1c9d84",
          "fullHash": "1ef0798b0189870d616c9157e62c02deaf56833c323137e10023eeda6819932e"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.azure.store.AzureStoreBlob",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.azure.store.AzureStoreBlob",
      "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreBlob",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreBlob",
        "parameters": [],
        "name": "AzureStoreBlob",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreBlob",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreBlob",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreBlob"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreBlob",
          "parameters": [],
          "name": "AzureStoreBlob",
          "parameterizedName": "com.orbitalhq.azure.store.AzureStoreBlob",
          "namespace": "com.orbitalhq.azure.store",
          "shortDisplayName": "AzureStoreBlob",
          "longDisplayName": "com.orbitalhq.azure.store.AzureStoreBlob"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
        "parameters": [],
        "name": "AwsLambdaService",
        "parameterizedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "AwsLambdaService",
        "longDisplayName": "com.orbitalhq.aws.lambda.AwsLambdaService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "version": "0.0.0",
          "content": "annotation AwsLambdaService {\n      connectionName : ConnectionName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors:0.0.0",
          "contentHash": "1459a4",
          "fullHash": "78c2ca4fade2f37b11d6974fd64669905ddfb018787a181bc80b3b3d800e045d"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.lambda.AwsLambdaService",
      "fullyQualifiedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
        "parameters": [],
        "name": "AwsLambdaService",
        "parameterizedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "AwsLambdaService",
        "longDisplayName": "com.orbitalhq.aws.lambda.AwsLambdaService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
          "parameters": [],
          "name": "AwsLambdaService",
          "parameterizedName": "com.orbitalhq.aws.lambda.AwsLambdaService",
          "namespace": "com.orbitalhq.aws.lambda",
          "shortDisplayName": "AwsLambdaService",
          "longDisplayName": "com.orbitalhq.aws.lambda.AwsLambdaService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.LambdaOperation",
        "parameters": [],
        "name": "LambdaOperation",
        "parameterizedName": "com.orbitalhq.aws.lambda.LambdaOperation",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "LambdaOperation",
        "longDisplayName": "com.orbitalhq.aws.lambda.LambdaOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "version": "0.0.0",
          "content": "annotation LambdaOperation {\n      name : OperationName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors:0.0.0",
          "contentHash": "ecf630",
          "fullHash": "7bb2b4db1cf3d9c6587275ad6b02b7caaf0c05ace9a5d5c2ad8e2b71b9d8a523"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.lambda.LambdaOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.lambda.LambdaOperation",
      "fullyQualifiedName": "com.orbitalhq.aws.lambda.LambdaOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.LambdaOperation",
        "parameters": [],
        "name": "LambdaOperation",
        "parameterizedName": "com.orbitalhq.aws.lambda.LambdaOperation",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "LambdaOperation",
        "longDisplayName": "com.orbitalhq.aws.lambda.LambdaOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.lambda.LambdaOperation",
          "parameters": [],
          "name": "LambdaOperation",
          "parameterizedName": "com.orbitalhq.aws.lambda.LambdaOperation",
          "namespace": "com.orbitalhq.aws.lambda",
          "shortDisplayName": "LambdaOperation",
          "longDisplayName": "com.orbitalhq.aws.lambda.LambdaOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.dynamo.ConnectionName",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.dynamo.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "version": "0.0.0",
          "content": "type ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors:0.0.0",
          "contentHash": "716d89",
          "fullHash": "a7f4b92a73076f67d3c0b4cc469e9b4188b00e4c6e52aad2ee8884dba9b8bb7c"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.dynamo.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.dynamo.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.aws.dynamo.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.dynamo.ConnectionName",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.dynamo.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.dynamo.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.aws.dynamo.ConnectionName",
          "namespace": "com.orbitalhq.aws.dynamo",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.aws.dynamo.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.DynamoService",
        "parameters": [],
        "name": "DynamoService",
        "parameterizedName": "com.orbitalhq.aws.dynamo.DynamoService",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "DynamoService",
        "longDisplayName": "com.orbitalhq.aws.dynamo.DynamoService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "version": "0.0.0",
          "content": "annotation DynamoService {\n    }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors:0.0.0",
          "contentHash": "d24382",
          "fullHash": "7c451cd19ff80800dd29cdd097e2814fb672c452d55d06888546d13feca58311"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.dynamo.DynamoService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.dynamo.DynamoService",
      "fullyQualifiedName": "com.orbitalhq.aws.dynamo.DynamoService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.DynamoService",
        "parameters": [],
        "name": "DynamoService",
        "parameterizedName": "com.orbitalhq.aws.dynamo.DynamoService",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "DynamoService",
        "longDisplayName": "com.orbitalhq.aws.dynamo.DynamoService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.dynamo.DynamoService",
          "parameters": [],
          "name": "DynamoService",
          "parameterizedName": "com.orbitalhq.aws.dynamo.DynamoService",
          "namespace": "com.orbitalhq.aws.dynamo",
          "shortDisplayName": "DynamoService",
          "longDisplayName": "com.orbitalhq.aws.dynamo.DynamoService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.Table",
        "parameters": [],
        "name": "Table",
        "parameterizedName": "com.orbitalhq.aws.dynamo.Table",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "Table",
        "longDisplayName": "com.orbitalhq.aws.dynamo.Table"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "version": "0.0.0",
          "content": "annotation Table {\n        connectionName : ConnectionName\n        tableName : TableName inherits String\n    }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors:0.0.0",
          "contentHash": "08b5be",
          "fullHash": "bd1c9c558484f2d1aa3e0289ac02e6d814b8470f81be0211d5353e4b5338ebd5"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.dynamo.Table",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.aws.dynamo.Table",
      "fullyQualifiedName": "com.orbitalhq.aws.dynamo.Table",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.Table",
        "parameters": [],
        "name": "Table",
        "parameterizedName": "com.orbitalhq.aws.dynamo.Table",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "Table",
        "longDisplayName": "com.orbitalhq.aws.dynamo.Table"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.dynamo.Table",
          "parameters": [],
          "name": "Table",
          "parameterizedName": "com.orbitalhq.aws.dynamo.Table",
          "namespace": "com.orbitalhq.aws.dynamo",
          "shortDisplayName": "Table",
          "longDisplayName": "com.orbitalhq.aws.dynamo.Table"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.formats.Csv",
        "parameters": [],
        "name": "Csv",
        "parameterizedName": "com.orbitalhq.formats.Csv",
        "namespace": "com.orbitalhq.formats",
        "shortDisplayName": "Csv",
        "longDisplayName": "com.orbitalhq.formats.Csv"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/CsvFormat",
          "version": "0.0.0",
          "content": "annotation Csv {\n      delimiter : String = \",\"\n      firstRecordAsHeader : Boolean = true\n      nullValue : String?\n      containsTrailingDelimiters : Boolean = false\n      useFieldNamesAsColumnNames: Boolean = false\n      quoteChar: String = '\"'\n      recordSeparator: String = \"\\r\\n\"\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/CsvFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/CsvFormat:0.0.0",
          "contentHash": "450303",
          "fullHash": "ad5c7e953321e7d983849afcae593b60319e458de2e746c8cd06f9a3b5b0882e"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.formats.Csv",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.formats.Csv",
      "fullyQualifiedName": "com.orbitalhq.formats.Csv",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.formats.Csv",
        "parameters": [],
        "name": "Csv",
        "parameterizedName": "com.orbitalhq.formats.Csv",
        "namespace": "com.orbitalhq.formats",
        "shortDisplayName": "Csv",
        "longDisplayName": "com.orbitalhq.formats.Csv"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.formats.Csv",
          "parameters": [],
          "name": "Csv",
          "parameterizedName": "com.orbitalhq.formats.Csv",
          "namespace": "com.orbitalhq.formats",
          "shortDisplayName": "Csv",
          "longDisplayName": "com.orbitalhq.formats.Csv"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.formats.Xml",
        "parameters": [],
        "name": "Xml",
        "parameterizedName": "com.orbitalhq.formats.Xml",
        "namespace": "com.orbitalhq.formats",
        "shortDisplayName": "Xml",
        "longDisplayName": "com.orbitalhq.formats.Xml"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "version": "0.0.0",
          "content": "annotation Xml {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/XmlFormat:0.0.0",
          "contentHash": "893bb8",
          "fullHash": "3c77a4afbd65560e8ef4e0ef6b18f2fd1254144a749009d6a7facfc9401d6e86"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.formats.Xml",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.formats.Xml",
      "fullyQualifiedName": "com.orbitalhq.formats.Xml",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.formats.Xml",
        "parameters": [],
        "name": "Xml",
        "parameterizedName": "com.orbitalhq.formats.Xml",
        "namespace": "com.orbitalhq.formats",
        "shortDisplayName": "Xml",
        "longDisplayName": "com.orbitalhq.formats.Xml"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.formats.Xml",
          "parameters": [],
          "name": "Xml",
          "parameterizedName": "com.orbitalhq.formats.Xml",
          "namespace": "com.orbitalhq.formats",
          "shortDisplayName": "Xml",
          "longDisplayName": "com.orbitalhq.formats.Xml"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "lang.taxi.xml.XmlAttribute",
        "parameters": [],
        "name": "XmlAttribute",
        "parameterizedName": "lang.taxi.xml.XmlAttribute",
        "namespace": "lang.taxi.xml",
        "shortDisplayName": "XmlAttribute",
        "longDisplayName": "lang.taxi.xml.XmlAttribute"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "version": "0.0.0",
          "content": "annotation XmlAttribute",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/XmlFormat:0.0.0",
          "contentHash": "403fdd",
          "fullHash": "93eb5df5e0b437f71e20407f8f59c009b1a338510c0e300900b0db3d66eb7121"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "lang.taxi.xml.XmlAttribute",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "lang.taxi.xml.XmlAttribute",
      "fullyQualifiedName": "lang.taxi.xml.XmlAttribute",
      "memberQualifiedName": {
        "fullyQualifiedName": "lang.taxi.xml.XmlAttribute",
        "parameters": [],
        "name": "XmlAttribute",
        "parameterizedName": "lang.taxi.xml.XmlAttribute",
        "namespace": "lang.taxi.xml",
        "shortDisplayName": "XmlAttribute",
        "longDisplayName": "lang.taxi.xml.XmlAttribute"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "lang.taxi.xml.XmlAttribute",
          "parameters": [],
          "name": "XmlAttribute",
          "parameterizedName": "lang.taxi.xml.XmlAttribute",
          "namespace": "lang.taxi.xml",
          "shortDisplayName": "XmlAttribute",
          "longDisplayName": "lang.taxi.xml.XmlAttribute"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "lang.taxi.xml.XmlBody",
        "parameters": [],
        "name": "XmlBody",
        "parameterizedName": "lang.taxi.xml.XmlBody",
        "namespace": "lang.taxi.xml",
        "shortDisplayName": "XmlBody",
        "longDisplayName": "lang.taxi.xml.XmlBody"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "version": "0.0.0",
          "content": "annotation XmlBody",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/XmlFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/XmlFormat:0.0.0",
          "contentHash": "658ba6",
          "fullHash": "ac3c3279c628c30cbdefb9e21651232d593140f51edc515eb925929dbd52edd6"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "lang.taxi.xml.XmlBody",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "lang.taxi.xml.XmlBody",
      "fullyQualifiedName": "lang.taxi.xml.XmlBody",
      "memberQualifiedName": {
        "fullyQualifiedName": "lang.taxi.xml.XmlBody",
        "parameters": [],
        "name": "XmlBody",
        "parameterizedName": "lang.taxi.xml.XmlBody",
        "namespace": "lang.taxi.xml",
        "shortDisplayName": "XmlBody",
        "longDisplayName": "lang.taxi.xml.XmlBody"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "lang.taxi.xml.XmlBody",
          "parameters": [],
          "name": "XmlBody",
          "parameterizedName": "lang.taxi.xml.XmlBody",
          "namespace": "lang.taxi.xml",
          "shortDisplayName": "XmlBody",
          "longDisplayName": "lang.taxi.xml.XmlBody"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
        "parameters": [],
        "name": "RetryTimeInSeconds",
        "parameterizedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "RetryTimeInSeconds",
        "longDisplayName": "com.orbitalhq.http.operations.RetryTimeInSeconds"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "type RetryTimeInSeconds inherits Int",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "0ca96d",
          "fullHash": "48dae6435592d18e533f9a73df3dc05167422c574157ed45e84a904a3f78e615"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
      "fullyQualifiedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
        "parameters": [],
        "name": "RetryTimeInSeconds",
        "parameterizedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "RetryTimeInSeconds",
        "longDisplayName": "com.orbitalhq.http.operations.RetryTimeInSeconds"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
          "parameters": [],
          "name": "RetryTimeInSeconds",
          "parameterizedName": "com.orbitalhq.http.operations.RetryTimeInSeconds",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "RetryTimeInSeconds",
          "longDisplayName": "com.orbitalhq.http.operations.RetryTimeInSeconds"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.MaximumRetries",
        "parameters": [],
        "name": "MaximumRetries",
        "parameterizedName": "com.orbitalhq.http.operations.MaximumRetries",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "MaximumRetries",
        "longDisplayName": "com.orbitalhq.http.operations.MaximumRetries"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "type MaximumRetries inherits Int",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "25e741",
          "fullHash": "b2c7b7813bc723d29313a88a7993443aaf00a6ed579824481e6619f5c6e774b4"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.MaximumRetries",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.MaximumRetries",
      "fullyQualifiedName": "com.orbitalhq.http.operations.MaximumRetries",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.MaximumRetries",
        "parameters": [],
        "name": "MaximumRetries",
        "parameterizedName": "com.orbitalhq.http.operations.MaximumRetries",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "MaximumRetries",
        "longDisplayName": "com.orbitalhq.http.operations.MaximumRetries"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.MaximumRetries",
          "parameters": [],
          "name": "MaximumRetries",
          "parameterizedName": "com.orbitalhq.http.operations.MaximumRetries",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "MaximumRetries",
          "longDisplayName": "com.orbitalhq.http.operations.MaximumRetries"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
        "parameters": [],
        "name": "ExponentialBackOffJitter",
        "parameterizedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "ExponentialBackOffJitter",
        "longDisplayName": "com.orbitalhq.http.operations.ExponentialBackOffJitter"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "shortDisplayName": "Decimal",
          "longDisplayName": "lang.taxi.Decimal"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "type ExponentialBackOffJitter inherits Decimal",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "3785f9",
          "fullHash": "1316f6814c124715faaa37d145f1caa104e7d05f02306a81475fbdcccb7b7450"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Decimal",
        "parameters": [],
        "name": "Decimal",
        "parameterizedName": "lang.taxi.Decimal",
        "namespace": "lang.taxi",
        "shortDisplayName": "Decimal",
        "longDisplayName": "lang.taxi.Decimal"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
      "fullyQualifiedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
        "parameters": [],
        "name": "ExponentialBackOffJitter",
        "parameterizedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "ExponentialBackOffJitter",
        "longDisplayName": "com.orbitalhq.http.operations.ExponentialBackOffJitter"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
          "parameters": [],
          "name": "ExponentialBackOffJitter",
          "parameterizedName": "com.orbitalhq.http.operations.ExponentialBackOffJitter",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "ExponentialBackOffJitter",
          "longDisplayName": "com.orbitalhq.http.operations.ExponentialBackOffJitter"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.ResponseCode",
        "parameters": [],
        "name": "ResponseCode",
        "parameterizedName": "com.orbitalhq.http.operations.ResponseCode",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "ResponseCode",
        "longDisplayName": "com.orbitalhq.http.operations.ResponseCode"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "type ResponseCode inherits Int",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "820cab",
          "fullHash": "352ff0bbfd708287d7170c21e2710cac529a28982b8b1c9ef0758ac64c8c5fdc"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.ResponseCode",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.ResponseCode",
      "fullyQualifiedName": "com.orbitalhq.http.operations.ResponseCode",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.ResponseCode",
        "parameters": [],
        "name": "ResponseCode",
        "parameterizedName": "com.orbitalhq.http.operations.ResponseCode",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "ResponseCode",
        "longDisplayName": "com.orbitalhq.http.operations.ResponseCode"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.ResponseCode",
          "parameters": [],
          "name": "ResponseCode",
          "parameterizedName": "com.orbitalhq.http.operations.ResponseCode",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "ResponseCode",
          "longDisplayName": "com.orbitalhq.http.operations.ResponseCode"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
        "parameters": [],
        "name": "HttpFixedRetryPolicy",
        "parameterizedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpFixedRetryPolicy",
        "longDisplayName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "annotation HttpFixedRetryPolicy {\n       maxRetries: MaximumRetries\n       retryDelay: RetryTimeInSeconds\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "cea606",
          "fullHash": "01ed53741e0c47c60cab0a4a0589e344d95f318025072394e39f44dd663cd7eb"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
      "fullyQualifiedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
        "parameters": [],
        "name": "HttpFixedRetryPolicy",
        "parameterizedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpFixedRetryPolicy",
        "longDisplayName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
          "parameters": [],
          "name": "HttpFixedRetryPolicy",
          "parameterizedName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "HttpFixedRetryPolicy",
          "longDisplayName": "com.orbitalhq.http.operations.HttpFixedRetryPolicy"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
        "parameters": [],
        "name": "HttpExponentialRetryPolicy",
        "parameterizedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpExponentialRetryPolicy",
        "longDisplayName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "annotation HttpExponentialRetryPolicy {\n       maxRetries: MaximumRetries\n       retryDelay: RetryTimeInSeconds\n       jitter: ExponentialBackOffJitter = 0.5\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "1e4d6f",
          "fullHash": "4c0bdef8322ef76e03b6534bd7e24c4b02c73457a3f3e2d0f5b8376d328a4a65"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
      "fullyQualifiedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
        "parameters": [],
        "name": "HttpExponentialRetryPolicy",
        "parameterizedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpExponentialRetryPolicy",
        "longDisplayName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
          "parameters": [],
          "name": "HttpExponentialRetryPolicy",
          "parameterizedName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "HttpExponentialRetryPolicy",
          "longDisplayName": "com.orbitalhq.http.operations.HttpExponentialRetryPolicy"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpRetry",
        "parameters": [],
        "name": "HttpRetry",
        "parameterizedName": "com.orbitalhq.http.operations.HttpRetry",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpRetry",
        "longDisplayName": "com.orbitalhq.http.operations.HttpRetry"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "version": "0.0.0",
          "content": "annotation HttpRetry {\n      fixedRetryPolicy: com.orbitalhq.http.operations.HttpFixedRetryPolicy?\n      exponentialRetryPolicy: com.orbitalhq.http.operations.HttpExponentialRetryPolicy?\n      responseCode: ResponseCode[]\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema",
          "id": "[com.orbitalhq/core-types/1.0.0]/HttpRetryAnnotationSchema:0.0.0",
          "contentHash": "506009",
          "fullHash": "e5074cf10bbcb2fc8e9795c3dbd4db22326121412b9321cbced453917839376b"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.http.operations.HttpRetry",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.http.operations.HttpRetry",
      "fullyQualifiedName": "com.orbitalhq.http.operations.HttpRetry",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.http.operations.HttpRetry",
        "parameters": [],
        "name": "HttpRetry",
        "parameterizedName": "com.orbitalhq.http.operations.HttpRetry",
        "namespace": "com.orbitalhq.http.operations",
        "shortDisplayName": "HttpRetry",
        "longDisplayName": "com.orbitalhq.http.operations.HttpRetry"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.http.operations.HttpRetry",
          "parameters": [],
          "name": "HttpRetry",
          "parameterizedName": "com.orbitalhq.http.operations.HttpRetry",
          "namespace": "com.orbitalhq.http.operations",
          "shortDisplayName": "HttpRetry",
          "longDisplayName": "com.orbitalhq.http.operations.HttpRetry"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastService",
        "parameters": [],
        "name": "HazelcastService",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastService",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastService",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation HazelcastService {\n      connectionName : String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "790c20",
          "fullHash": "ad3bbe8892501eda9ac4c9edda6694e81646fc6ebc68969d76f369849d2c8895"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.HazelcastService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.HazelcastService",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastService",
        "parameters": [],
        "name": "HazelcastService",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastService",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastService",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastService",
          "parameters": [],
          "name": "HazelcastService",
          "parameterizedName": "com.orbitalhq.hazelcast.HazelcastService",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "HazelcastService",
          "longDisplayName": "com.orbitalhq.hazelcast.HazelcastService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMap",
        "parameters": [],
        "name": "HazelcastMap",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMap",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastMap",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMap"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation HazelcastMap {\n      name : HazelcastMapName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "ec25d5",
          "fullHash": "84daa2e34eb4c04f74a88e15682fb3904e4bd75916882d5a5233063118965d96"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.HazelcastMap",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMap",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMap",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMap",
        "parameters": [],
        "name": "HazelcastMap",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMap",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastMap",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMap"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMap",
          "parameters": [],
          "name": "HazelcastMap",
          "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMap",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "HazelcastMap",
          "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMap"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.hazelcast.UpsertOperation",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.hazelcast.UpsertOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation UpsertOperation {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "970f39",
          "fullHash": "bb918680515a7a021a52c4646f127f855fb8ed1541d17ce3be1e0906b2d3d651"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.UpsertOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.UpsertOperation",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.UpsertOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.hazelcast.UpsertOperation",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.hazelcast.UpsertOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.UpsertOperation",
          "parameters": [],
          "name": "UpsertOperation",
          "parameterizedName": "com.orbitalhq.hazelcast.UpsertOperation",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "UpsertOperation",
          "longDisplayName": "com.orbitalhq.hazelcast.UpsertOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.DeleteOperation",
        "parameters": [],
        "name": "DeleteOperation",
        "parameterizedName": "com.orbitalhq.hazelcast.DeleteOperation",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "DeleteOperation",
        "longDisplayName": "com.orbitalhq.hazelcast.DeleteOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation DeleteOperation {\n      mapName : String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "6d7146",
          "fullHash": "c5884f306debbc4d67ae85b3bc0fbbcf76025a5f972326c4e5a3cc55bbffb256"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.DeleteOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.DeleteOperation",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.DeleteOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.DeleteOperation",
        "parameters": [],
        "name": "DeleteOperation",
        "parameterizedName": "com.orbitalhq.hazelcast.DeleteOperation",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "DeleteOperation",
        "longDisplayName": "com.orbitalhq.hazelcast.DeleteOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.DeleteOperation",
          "parameters": [],
          "name": "DeleteOperation",
          "parameterizedName": "com.orbitalhq.hazelcast.DeleteOperation",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "DeleteOperation",
          "longDisplayName": "com.orbitalhq.hazelcast.DeleteOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.JsonObject",
        "parameters": [],
        "name": "JsonObject",
        "parameterizedName": "com.orbitalhq.hazelcast.JsonObject",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "JsonObject",
        "longDisplayName": "com.orbitalhq.hazelcast.JsonObject"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation JsonObject",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "3ccc16",
          "fullHash": "2663da80dff1cad95c5bd58d54084bcdcb1518dda88b1051039075fcbc0ec9ec"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.JsonObject",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.JsonObject",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.JsonObject",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.JsonObject",
        "parameters": [],
        "name": "JsonObject",
        "parameterizedName": "com.orbitalhq.hazelcast.JsonObject",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "JsonObject",
        "longDisplayName": "com.orbitalhq.hazelcast.JsonObject"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.JsonObject",
          "parameters": [],
          "name": "JsonObject",
          "parameterizedName": "com.orbitalhq.hazelcast.JsonObject",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "JsonObject",
          "longDisplayName": "com.orbitalhq.hazelcast.JsonObject"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.CompactObject",
        "parameters": [],
        "name": "CompactObject",
        "parameterizedName": "com.orbitalhq.hazelcast.CompactObject",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "CompactObject",
        "longDisplayName": "com.orbitalhq.hazelcast.CompactObject"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "annotation CompactObject",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "1939b0",
          "fullHash": "4bdaf846b68f707a80db58594cba5532012090222a3bc7e5ac6c00d26e7fd6e2"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.CompactObject",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.hazelcast.CompactObject",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.CompactObject",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.CompactObject",
        "parameters": [],
        "name": "CompactObject",
        "parameterizedName": "com.orbitalhq.hazelcast.CompactObject",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "CompactObject",
        "longDisplayName": "com.orbitalhq.hazelcast.CompactObject"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.CompactObject",
          "parameters": [],
          "name": "CompactObject",
          "parameterizedName": "com.orbitalhq.hazelcast.CompactObject",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "CompactObject",
          "longDisplayName": "com.orbitalhq.hazelcast.CompactObject"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.mongo.ConnectionName",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.mongo.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "type ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "716d89",
          "fullHash": "e5a035ac8e584fb9ec46b811b45b4fc5c7e5a7a35f399259ffb374e754a88ab4"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.mongo.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.mongo.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.mongo.ConnectionName",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.mongo.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.mongo.ConnectionName",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.mongo.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.BatchSize",
        "parameters": [],
        "name": "BatchSize",
        "parameterizedName": "com.orbitalhq.mongo.BatchSize",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "BatchSize",
        "longDisplayName": "com.orbitalhq.mongo.BatchSize"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "type BatchSize inherits Int",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "6b0800",
          "fullHash": "184af049a219ccbe23bd51b47ddc737cbb003fad4de4a602f295e4f383f361d4"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.BatchSize",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.BatchSize",
      "fullyQualifiedName": "com.orbitalhq.mongo.BatchSize",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.BatchSize",
        "parameters": [],
        "name": "BatchSize",
        "parameterizedName": "com.orbitalhq.mongo.BatchSize",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "BatchSize",
        "longDisplayName": "com.orbitalhq.mongo.BatchSize"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.BatchSize",
          "parameters": [],
          "name": "BatchSize",
          "parameterizedName": "com.orbitalhq.mongo.BatchSize",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "BatchSize",
          "longDisplayName": "com.orbitalhq.mongo.BatchSize"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.BatchDuration",
        "parameters": [],
        "name": "BatchDuration",
        "parameterizedName": "com.orbitalhq.mongo.BatchDuration",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "BatchDuration",
        "longDisplayName": "com.orbitalhq.mongo.BatchDuration"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "type BatchDuration inherits Int",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "64fd46",
          "fullHash": "055dbcb2e62755055ff64d222f06e37b20d238222a07b6575364879bf5c44597"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.BatchDuration",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.BatchDuration",
      "fullyQualifiedName": "com.orbitalhq.mongo.BatchDuration",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.BatchDuration",
        "parameters": [],
        "name": "BatchDuration",
        "parameterizedName": "com.orbitalhq.mongo.BatchDuration",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "BatchDuration",
        "longDisplayName": "com.orbitalhq.mongo.BatchDuration"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.BatchDuration",
          "parameters": [],
          "name": "BatchDuration",
          "parameterizedName": "com.orbitalhq.mongo.BatchDuration",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "BatchDuration",
          "longDisplayName": "com.orbitalhq.mongo.BatchDuration"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.MongoService",
        "parameters": [],
        "name": "MongoService",
        "parameterizedName": "com.orbitalhq.mongo.MongoService",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "MongoService",
        "longDisplayName": "com.orbitalhq.mongo.MongoService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "annotation MongoService {\n      connection : ConnectionName\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "7ddaaa",
          "fullHash": "600bdb901d49a38c5e0774c5eb3120d234b0df59ab5fb6496c91edb4ddc3ac5b"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.MongoService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.MongoService",
      "fullyQualifiedName": "com.orbitalhq.mongo.MongoService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.MongoService",
        "parameters": [],
        "name": "MongoService",
        "parameterizedName": "com.orbitalhq.mongo.MongoService",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "MongoService",
        "longDisplayName": "com.orbitalhq.mongo.MongoService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.MongoService",
          "parameters": [],
          "name": "MongoService",
          "parameterizedName": "com.orbitalhq.mongo.MongoService",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "MongoService",
          "longDisplayName": "com.orbitalhq.mongo.MongoService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.mongo.UpsertOperation",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.mongo.UpsertOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "annotation UpsertOperation {\n        batchSize: BatchSize?\n        batchDuration: BatchDuration?\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "c7eece",
          "fullHash": "cd437a26bb3f62954ce545e0e3332fc34ea00bd671669ce85add06db6e50bd07"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.UpsertOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.UpsertOperation",
      "fullyQualifiedName": "com.orbitalhq.mongo.UpsertOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.UpsertOperation",
        "parameters": [],
        "name": "UpsertOperation",
        "parameterizedName": "com.orbitalhq.mongo.UpsertOperation",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "UpsertOperation",
        "longDisplayName": "com.orbitalhq.mongo.UpsertOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.UpsertOperation",
          "parameters": [],
          "name": "UpsertOperation",
          "parameterizedName": "com.orbitalhq.mongo.UpsertOperation",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "UpsertOperation",
          "longDisplayName": "com.orbitalhq.mongo.UpsertOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.ObjectId",
        "parameters": [],
        "name": "ObjectId",
        "parameterizedName": "com.orbitalhq.mongo.ObjectId",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "ObjectId",
        "longDisplayName": "com.orbitalhq.mongo.ObjectId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "annotation ObjectId {}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "ae81e5",
          "fullHash": "9e96cec6868d0ad35cccfff2bd875d6a1beb4d40e3b01a0c6804d216f2d8a488"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.ObjectId",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.ObjectId",
      "fullyQualifiedName": "com.orbitalhq.mongo.ObjectId",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.ObjectId",
        "parameters": [],
        "name": "ObjectId",
        "parameterizedName": "com.orbitalhq.mongo.ObjectId",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "ObjectId",
        "longDisplayName": "com.orbitalhq.mongo.ObjectId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.ObjectId",
          "parameters": [],
          "name": "ObjectId",
          "parameterizedName": "com.orbitalhq.mongo.ObjectId",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "ObjectId",
          "longDisplayName": "com.orbitalhq.mongo.ObjectId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.Collection",
        "parameters": [],
        "name": "Collection",
        "parameterizedName": "com.orbitalhq.mongo.Collection",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "Collection",
        "longDisplayName": "com.orbitalhq.mongo.Collection"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "annotation Collection {\n      connection : ConnectionName\n      collection : CollectionName inherits String\n   }",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "349f10",
          "fullHash": "de9e11423ca81b1438389685ff7e2c64725b6edc18b945f0fa7f6a52161b4078"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.Collection",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.mongo.Collection",
      "fullyQualifiedName": "com.orbitalhq.mongo.Collection",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.Collection",
        "parameters": [],
        "name": "Collection",
        "parameterizedName": "com.orbitalhq.mongo.Collection",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "Collection",
        "longDisplayName": "com.orbitalhq.mongo.Collection"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.Collection",
          "parameters": [],
          "name": "Collection",
          "parameterizedName": "com.orbitalhq.mongo.Collection",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "Collection",
          "longDisplayName": "com.orbitalhq.mongo.Collection"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "lang.taxi.formats.AvroMessage",
        "parameters": [],
        "name": "AvroMessage",
        "parameterizedName": "lang.taxi.formats.AvroMessage",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroMessage",
        "longDisplayName": "lang.taxi.formats.AvroMessage"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "version": "0.0.0",
          "content": "annotation AvroMessage",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/AvroFormat:0.0.0",
          "contentHash": "90876c",
          "fullHash": "5f0bc4598ebbc9299de232e8b8fb5a4114bea884347385fd6ece3c7d79937898"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "lang.taxi.formats.AvroMessage",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "lang.taxi.formats.AvroMessage",
      "fullyQualifiedName": "lang.taxi.formats.AvroMessage",
      "memberQualifiedName": {
        "fullyQualifiedName": "lang.taxi.formats.AvroMessage",
        "parameters": [],
        "name": "AvroMessage",
        "parameterizedName": "lang.taxi.formats.AvroMessage",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroMessage",
        "longDisplayName": "lang.taxi.formats.AvroMessage"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "lang.taxi.formats.AvroMessage",
          "parameters": [],
          "name": "AvroMessage",
          "parameterizedName": "lang.taxi.formats.AvroMessage",
          "namespace": "lang.taxi.formats",
          "shortDisplayName": "AvroMessage",
          "longDisplayName": "lang.taxi.formats.AvroMessage"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "lang.taxi.formats.AvroMessageWrapper",
        "parameters": [],
        "name": "AvroMessageWrapper",
        "parameterizedName": "lang.taxi.formats.AvroMessageWrapper",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroMessageWrapper",
        "longDisplayName": "lang.taxi.formats.AvroMessageWrapper"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "version": "0.0.0",
          "content": "[[\n   Indicates that a model is a wrapper around an AvroMessage.\n   This should be used when embedding an Avro message within another message, such as to capture\n   Kafka message keys or headers.\n\n  The message payload will be treated as an Avro message, even if an Avro schema is not explicitly declared for this type.\n\n   Any fields declared within this message, aside from the Avro message itself, must be populated\n   through expressions or meta-value suppliers (e.g., KafkaMessageKey or KafkaMessageHeader).\n   ]]\n   annotation AvroMessageWrapper",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/AvroFormat:0.0.0",
          "contentHash": "df9f96",
          "fullHash": "53db69bb24e2088ee0acf0e7dffbecc3a940e809e0ce72a0846911229f16de50"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "lang.taxi.formats.AvroMessageWrapper",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "lang.taxi.formats.AvroMessageWrapper",
      "fullyQualifiedName": "lang.taxi.formats.AvroMessageWrapper",
      "memberQualifiedName": {
        "fullyQualifiedName": "lang.taxi.formats.AvroMessageWrapper",
        "parameters": [],
        "name": "AvroMessageWrapper",
        "parameterizedName": "lang.taxi.formats.AvroMessageWrapper",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroMessageWrapper",
        "longDisplayName": "lang.taxi.formats.AvroMessageWrapper"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "lang.taxi.formats.AvroMessageWrapper",
          "parameters": [],
          "name": "AvroMessageWrapper",
          "parameterizedName": "lang.taxi.formats.AvroMessageWrapper",
          "namespace": "lang.taxi.formats",
          "shortDisplayName": "AvroMessageWrapper",
          "longDisplayName": "lang.taxi.formats.AvroMessageWrapper"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "lang.taxi.formats.AvroField",
        "parameters": [],
        "name": "AvroField",
        "parameterizedName": "lang.taxi.formats.AvroField",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroField",
        "longDisplayName": "lang.taxi.formats.AvroField"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "version": "0.0.0",
          "content": "annotation AvroField {\n ordinal: Int\n}",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AvroFormat",
          "id": "[com.orbitalhq/core-types/1.0.0]/AvroFormat:0.0.0",
          "contentHash": "94245e",
          "fullHash": "faf08bcc426e3ed4a74d80aae295e1c3de86568e4d7a54eec82427f45afdb569"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "lang.taxi.formats.AvroField",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "lang.taxi.formats.AvroField",
      "fullyQualifiedName": "lang.taxi.formats.AvroField",
      "memberQualifiedName": {
        "fullyQualifiedName": "lang.taxi.formats.AvroField",
        "parameters": [],
        "name": "AvroField",
        "parameterizedName": "lang.taxi.formats.AvroField",
        "namespace": "lang.taxi.formats",
        "shortDisplayName": "AvroField",
        "longDisplayName": "lang.taxi.formats.AvroField"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "lang.taxi.formats.AvroField",
          "parameters": [],
          "name": "AvroField",
          "parameterizedName": "lang.taxi.formats.AvroField",
          "namespace": "lang.taxi.formats",
          "shortDisplayName": "AvroField",
          "longDisplayName": "lang.taxi.formats.AvroField"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.TableName",
        "parameters": [],
        "name": "TableName",
        "parameterizedName": "com.orbitalhq.jdbc.TableName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "TableName",
        "longDisplayName": "com.orbitalhq.jdbc.TableName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "TableName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "831e39",
          "fullHash": "4239ce8678a94d895df684de70214f6649212c6484dabffa6078098b4c3647ca"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.TableName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.jdbc.TableName",
      "fullyQualifiedName": "com.orbitalhq.jdbc.TableName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.TableName",
        "parameters": [],
        "name": "TableName",
        "parameterizedName": "com.orbitalhq.jdbc.TableName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "TableName",
        "longDisplayName": "com.orbitalhq.jdbc.TableName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.TableName",
          "parameters": [],
          "name": "TableName",
          "parameterizedName": "com.orbitalhq.jdbc.TableName",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "TableName",
          "longDisplayName": "com.orbitalhq.jdbc.TableName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.SchemaName",
        "parameters": [],
        "name": "SchemaName",
        "parameterizedName": "com.orbitalhq.jdbc.SchemaName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "SchemaName",
        "longDisplayName": "com.orbitalhq.jdbc.SchemaName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "version": "0.0.0",
          "content": "SchemaName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/JdbcConnectors:0.0.0",
          "contentHash": "54baec",
          "fullHash": "9505f810eddce527157495b70235305d1a915a6ddda3740412536844630c3437"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.jdbc.SchemaName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.jdbc.SchemaName",
      "fullyQualifiedName": "com.orbitalhq.jdbc.SchemaName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.jdbc.SchemaName",
        "parameters": [],
        "name": "SchemaName",
        "parameterizedName": "com.orbitalhq.jdbc.SchemaName",
        "namespace": "com.orbitalhq.jdbc",
        "shortDisplayName": "SchemaName",
        "longDisplayName": "com.orbitalhq.jdbc.SchemaName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.jdbc.SchemaName",
          "parameters": [],
          "name": "SchemaName",
          "parameterizedName": "com.orbitalhq.jdbc.SchemaName",
          "namespace": "com.orbitalhq.jdbc",
          "shortDisplayName": "SchemaName",
          "longDisplayName": "com.orbitalhq.jdbc.SchemaName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.s3.ConnectionName",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.s3.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "58fe9c",
          "fullHash": "b37301d1a36ce0bde86c217ac14117fd4a05bc09c5452efb2d57ad0678f70210"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.s3.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.s3.ConnectionName",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.s3.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.aws.s3.ConnectionName",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.aws.s3.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.BucketName",
        "parameters": [],
        "name": "BucketName",
        "parameterizedName": "com.orbitalhq.aws.s3.BucketName",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "BucketName",
        "longDisplayName": "com.orbitalhq.aws.s3.BucketName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "version": "0.0.0",
          "content": "BucketName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsS3Connectors:0.0.0",
          "contentHash": "dd5070",
          "fullHash": "d77925fa41308f68491d736d952b1b0b1b5a0df7c16fb9f876fe5324dbe2396f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.s3.BucketName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.s3.BucketName",
      "fullyQualifiedName": "com.orbitalhq.aws.s3.BucketName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.s3.BucketName",
        "parameters": [],
        "name": "BucketName",
        "parameterizedName": "com.orbitalhq.aws.s3.BucketName",
        "namespace": "com.orbitalhq.aws.s3",
        "shortDisplayName": "BucketName",
        "longDisplayName": "com.orbitalhq.aws.s3.BucketName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.s3.BucketName",
          "parameters": [],
          "name": "BucketName",
          "parameterizedName": "com.orbitalhq.aws.s3.BucketName",
          "namespace": "com.orbitalhq.aws.s3",
          "shortDisplayName": "BucketName",
          "longDisplayName": "com.orbitalhq.aws.s3.BucketName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.sqs.ConnectionName",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.sqs.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "version": "0.0.0",
          "content": "ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors:0.0.0",
          "contentHash": "58fe9c",
          "fullHash": "4b00ae753af259b9e7b0ce252d91114619975bd8667aacec44799362924a9946"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.sqs.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.sqs.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.aws.sqs.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.sqs.ConnectionName",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.sqs.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.sqs.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.aws.sqs.ConnectionName",
          "namespace": "com.orbitalhq.aws.sqs",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.aws.sqs.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.QueueName",
        "parameters": [],
        "name": "QueueName",
        "parameterizedName": "com.orbitalhq.aws.sqs.QueueName",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "QueueName",
        "longDisplayName": "com.orbitalhq.aws.sqs.QueueName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "version": "0.0.0",
          "content": "QueueName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsSqsConnectors:0.0.0",
          "contentHash": "9fc77e",
          "fullHash": "fd5123d39d22e41a53fea9fbf712118f52d2e77623e4c6b1b697106325ba8e51"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.sqs.QueueName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.sqs.QueueName",
      "fullyQualifiedName": "com.orbitalhq.aws.sqs.QueueName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.sqs.QueueName",
        "parameters": [],
        "name": "QueueName",
        "parameterizedName": "com.orbitalhq.aws.sqs.QueueName",
        "namespace": "com.orbitalhq.aws.sqs",
        "shortDisplayName": "QueueName",
        "longDisplayName": "com.orbitalhq.aws.sqs.QueueName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.sqs.QueueName",
          "parameters": [],
          "name": "QueueName",
          "parameterizedName": "com.orbitalhq.aws.sqs.QueueName",
          "namespace": "com.orbitalhq.aws.sqs",
          "shortDisplayName": "QueueName",
          "longDisplayName": "com.orbitalhq.aws.sqs.QueueName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.azure.store.ConnectionName",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.azure.store.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "version": "0.0.0",
          "content": "ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors:0.0.0",
          "contentHash": "58fe9c",
          "fullHash": "4e1be1c9ae09bc643ae86edd16de5b10345a1a9712cdcdaf0a9b331996bb83f0"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.azure.store.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.azure.store.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.azure.store.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.azure.store.ConnectionName",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.azure.store.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.azure.store.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.azure.store.ConnectionName",
          "namespace": "com.orbitalhq.azure.store",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.azure.store.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreContainer",
        "parameters": [],
        "name": "AzureStoreContainer",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreContainer",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreContainer",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreContainer"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "version": "0.0.0",
          "content": "AzureStoreContainer inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AzureStoreConnectors:0.0.0",
          "contentHash": "fa3de2",
          "fullHash": "f9c38e3e9cb5575a318d667980039a7c821b8e584ede8a75225affdf3885b463"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.azure.store.AzureStoreContainer",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.azure.store.AzureStoreContainer",
      "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreContainer",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreContainer",
        "parameters": [],
        "name": "AzureStoreContainer",
        "parameterizedName": "com.orbitalhq.azure.store.AzureStoreContainer",
        "namespace": "com.orbitalhq.azure.store",
        "shortDisplayName": "AzureStoreContainer",
        "longDisplayName": "com.orbitalhq.azure.store.AzureStoreContainer"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.azure.store.AzureStoreContainer",
          "parameters": [],
          "name": "AzureStoreContainer",
          "parameterizedName": "com.orbitalhq.azure.store.AzureStoreContainer",
          "namespace": "com.orbitalhq.azure.store",
          "shortDisplayName": "AzureStoreContainer",
          "longDisplayName": "com.orbitalhq.azure.store.AzureStoreContainer"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.lambda.ConnectionName",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.lambda.ConnectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "version": "0.0.0",
          "content": "ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors:0.0.0",
          "contentHash": "58fe9c",
          "fullHash": "9652240f1492225a834a56865bb9efa4c57e5b84624d0802fcba0916910e879e"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.lambda.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.lambda.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.aws.lambda.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.aws.lambda.ConnectionName",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.aws.lambda.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.lambda.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.aws.lambda.ConnectionName",
          "namespace": "com.orbitalhq.aws.lambda",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.aws.lambda.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.OperationName",
        "parameters": [],
        "name": "OperationName",
        "parameterizedName": "com.orbitalhq.aws.lambda.OperationName",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "OperationName",
        "longDisplayName": "com.orbitalhq.aws.lambda.OperationName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "version": "0.0.0",
          "content": "OperationName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsLambdaConnectors:0.0.0",
          "contentHash": "be2ce3",
          "fullHash": "c23c3a0aa23d4ed8a103d55d6c95fb0123af3a15afa87004b07f19a193540e0f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.lambda.OperationName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.lambda.OperationName",
      "fullyQualifiedName": "com.orbitalhq.aws.lambda.OperationName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.lambda.OperationName",
        "parameters": [],
        "name": "OperationName",
        "parameterizedName": "com.orbitalhq.aws.lambda.OperationName",
        "namespace": "com.orbitalhq.aws.lambda",
        "shortDisplayName": "OperationName",
        "longDisplayName": "com.orbitalhq.aws.lambda.OperationName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.lambda.OperationName",
          "parameters": [],
          "name": "OperationName",
          "parameterizedName": "com.orbitalhq.aws.lambda.OperationName",
          "namespace": "com.orbitalhq.aws.lambda",
          "shortDisplayName": "OperationName",
          "longDisplayName": "com.orbitalhq.aws.lambda.OperationName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.TableName",
        "parameters": [],
        "name": "TableName",
        "parameterizedName": "com.orbitalhq.aws.dynamo.TableName",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "TableName",
        "longDisplayName": "com.orbitalhq.aws.dynamo.TableName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "version": "0.0.0",
          "content": "TableName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/AwsDynamoConnectors:0.0.0",
          "contentHash": "831e39",
          "fullHash": "31eee62819c935090eb09fb3aa78ba1b4fb1f27b693cbe587521cf1223f5d4ab"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.aws.dynamo.TableName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.aws.dynamo.TableName",
      "fullyQualifiedName": "com.orbitalhq.aws.dynamo.TableName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.aws.dynamo.TableName",
        "parameters": [],
        "name": "TableName",
        "parameterizedName": "com.orbitalhq.aws.dynamo.TableName",
        "namespace": "com.orbitalhq.aws.dynamo",
        "shortDisplayName": "TableName",
        "longDisplayName": "com.orbitalhq.aws.dynamo.TableName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.aws.dynamo.TableName",
          "parameters": [],
          "name": "TableName",
          "parameterizedName": "com.orbitalhq.aws.dynamo.TableName",
          "namespace": "com.orbitalhq.aws.dynamo",
          "shortDisplayName": "TableName",
          "longDisplayName": "com.orbitalhq.aws.dynamo.TableName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMapName",
        "parameters": [],
        "name": "HazelcastMapName",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMapName",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastMapName",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMapName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "version": "0.0.0",
          "content": "HazelcastMapName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors",
          "id": "[com.orbitalhq/core-types/1.0.0]/HazelcastConnectors:0.0.0",
          "contentHash": "4aea9f",
          "fullHash": "380ae3c68fb655f46d02d9949fb611d92cf328b81a41ebe3b561a0e9a2ca982d"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.hazelcast.HazelcastMapName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMapName",
      "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMapName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMapName",
        "parameters": [],
        "name": "HazelcastMapName",
        "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMapName",
        "namespace": "com.orbitalhq.hazelcast",
        "shortDisplayName": "HazelcastMapName",
        "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMapName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.hazelcast.HazelcastMapName",
          "parameters": [],
          "name": "HazelcastMapName",
          "parameterizedName": "com.orbitalhq.hazelcast.HazelcastMapName",
          "namespace": "com.orbitalhq.hazelcast",
          "shortDisplayName": "HazelcastMapName",
          "longDisplayName": "com.orbitalhq.hazelcast.HazelcastMapName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.mongo.CollectionName",
        "parameters": [],
        "name": "CollectionName",
        "parameterizedName": "com.orbitalhq.mongo.CollectionName",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "CollectionName",
        "longDisplayName": "com.orbitalhq.mongo.CollectionName"
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
          "name": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "version": "0.0.0",
          "content": "CollectionName inherits String",
          "packageIdentifier": {
            "organisation": "com.orbitalhq",
            "name": "core-types",
            "version": "1.0.0",
            "unversionedId": "com.orbitalhq/core-types",
            "id": "com.orbitalhq/core-types/1.0.0",
            "uriSafeId": "com.orbitalhq:core-types:1.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector",
          "id": "[com.orbitalhq/core-types/1.0.0]/MongoDbConnector:0.0.0",
          "contentHash": "f6a604",
          "fullHash": "3621c1065829e5cf8dbbc2b50d03f0b5557642ecd5d249d73a0795267557227a"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.mongo.CollectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.mongo.CollectionName",
      "fullyQualifiedName": "com.orbitalhq.mongo.CollectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.mongo.CollectionName",
        "parameters": [],
        "name": "CollectionName",
        "parameterizedName": "com.orbitalhq.mongo.CollectionName",
        "namespace": "com.orbitalhq.mongo",
        "shortDisplayName": "CollectionName",
        "longDisplayName": "com.orbitalhq.mongo.CollectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.mongo.CollectionName",
          "parameters": [],
          "name": "CollectionName",
          "parameterizedName": "com.orbitalhq.mongo.CollectionName",
          "namespace": "com.orbitalhq.mongo",
          "shortDisplayName": "CollectionName",
          "longDisplayName": "com.orbitalhq.mongo.CollectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
        "parameters": [],
        "name": "VyneQlQuery",
        "parameterizedName": "vyne.vyneQl.VyneQlQuery",
        "namespace": "vyne.vyneQl",
        "shortDisplayName": "VyneQlQuery",
        "longDisplayName": "vyne.vyneQl.VyneQlQuery"
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
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "type VyneQlQuery inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "fa32b1",
          "fullHash": "12664a25622d3a73c96c9f0f11fa7a805be4983c644de39b390d1e217c39a951"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "vyne.vyneQl.VyneQlQuery",
      "constraintsByPath": [],
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
      "longDisplayName": "vyne.vyneQl.VyneQlQuery",
      "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
      "memberQualifiedName": {
        "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
        "parameters": [],
        "name": "VyneQlQuery",
        "parameterizedName": "vyne.vyneQl.VyneQlQuery",
        "namespace": "vyne.vyneQl",
        "shortDisplayName": "VyneQlQuery",
        "longDisplayName": "vyne.vyneQl.VyneQlQuery"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
          "parameters": [],
          "name": "VyneQlQuery",
          "parameterizedName": "vyne.vyneQl.VyneQlQuery",
          "namespace": "vyne.vyneQl",
          "shortDisplayName": "VyneQlQuery",
          "longDisplayName": "vyne.vyneQl.VyneQlQuery"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaService",
        "parameters": [],
        "name": "KafkaService",
        "parameterizedName": "com.orbitalhq.kafka.KafkaService",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaService",
        "longDisplayName": "com.orbitalhq.kafka.KafkaService"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "annotation KafkaService {\r\n      connectionName : ConnectionName inherits String\r\n   }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "b5452a",
          "fullHash": "de7441e68c181f6d17ffc4a7c2dceaf45fd1271d72ef48261a8a597c84c0fd67"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaService",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.kafka.KafkaService",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaService",
        "parameters": [],
        "name": "KafkaService",
        "parameterizedName": "com.orbitalhq.kafka.KafkaService",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaService",
        "longDisplayName": "com.orbitalhq.kafka.KafkaService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "com.orbitalhq.kafka.KafkaService",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaService",
          "longDisplayName": "com.orbitalhq.kafka.KafkaService"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.TopicOffset",
        "parameters": [],
        "name": "TopicOffset",
        "parameterizedName": "com.orbitalhq.kafka.TopicOffset",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "TopicOffset",
        "longDisplayName": "com.orbitalhq.kafka.TopicOffset"
      },
      "attributes": {},
      "modifiers": [
        "ENUM"
      ],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [
        {
          "name": "earliest",
          "value": "earliest",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "latest",
          "value": "latest",
          "synonyms": [],
          "typeDoc": ""
        },
        {
          "name": "none",
          "value": "none",
          "synonyms": [],
          "typeDoc": ""
        }
      ],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "enum TopicOffset {\r\n      earliest,\r\n      latest,\r\n      none\r\n   }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "e0cd1c",
          "fullHash": "ed3777f3aa09dd5252ca36e85c9be228b25e2ada8a86d10377c20f2ecdc8b11a"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.TopicOffset",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "declaresFormat": false,
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
      "longDisplayName": "com.orbitalhq.kafka.TopicOffset",
      "fullyQualifiedName": "com.orbitalhq.kafka.TopicOffset",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.TopicOffset",
        "parameters": [],
        "name": "TopicOffset",
        "parameterizedName": "com.orbitalhq.kafka.TopicOffset",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "TopicOffset",
        "longDisplayName": "com.orbitalhq.kafka.TopicOffset"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.TopicOffset",
          "parameters": [],
          "name": "TopicOffset",
          "parameterizedName": "com.orbitalhq.kafka.TopicOffset",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "TopicOffset",
          "longDisplayName": "com.orbitalhq.kafka.TopicOffset"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
        "parameters": [],
        "name": "KafkaOperation",
        "parameterizedName": "com.orbitalhq.kafka.KafkaOperation",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaOperation",
        "longDisplayName": "com.orbitalhq.kafka.KafkaOperation"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "annotation KafkaOperation {\r\n      topic : TopicName inherits String\r\n      offset : TopicOffset\r\n   }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "1e96a9",
          "fullHash": "9055c84c87e0344631243d3eda76804fea6db23ec4a9bf0a7457befb748bf19f"
        }
      ],
      "typeParameters": [],
      "typeDoc": null,
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.KafkaOperation",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": null,
      "offset": null,
      "format": [],
      "hasFormat": false,
      "declaresFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "com.orbitalhq.kafka.KafkaOperation",
      "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
        "parameters": [],
        "name": "KafkaOperation",
        "parameterizedName": "com.orbitalhq.kafka.KafkaOperation",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "KafkaOperation",
        "longDisplayName": "com.orbitalhq.kafka.KafkaOperation"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
          "parameters": [],
          "name": "KafkaOperation",
          "parameterizedName": "com.orbitalhq.kafka.KafkaOperation",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "KafkaOperation",
          "longDisplayName": "com.orbitalhq.kafka.KafkaOperation"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "address.types.AddressId",
        "parameters": [],
        "name": "AddressId",
        "parameterizedName": "address.types.AddressId",
        "namespace": "address.types",
        "shortDisplayName": "AddressId",
        "longDisplayName": "address.types.AddressId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/address\\types\\AddressId.taxi",
          "version": "0.0.0",
          "content": "type AddressId inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/address\\types\\AddressId.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/address\\types\\AddressId.taxi:0.0.0",
          "contentHash": "828fc8",
          "fullHash": "5c59c50941952544159db240f966ca2fbec8f1273a50ffc5b2b3306a69caff7f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "address.types.AddressId",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "address.types.AddressId",
      "fullyQualifiedName": "address.types.AddressId",
      "memberQualifiedName": {
        "fullyQualifiedName": "address.types.AddressId",
        "parameters": [],
        "name": "AddressId",
        "parameterizedName": "address.types.AddressId",
        "namespace": "address.types",
        "shortDisplayName": "AddressId",
        "longDisplayName": "address.types.AddressId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "address.types.AddressId",
          "parameters": [],
          "name": "AddressId",
          "parameterizedName": "address.types.AddressId",
          "namespace": "address.types",
          "shortDisplayName": "AddressId",
          "longDisplayName": "address.types.AddressId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.Film",
        "parameters": [],
        "name": "Film",
        "parameterizedName": "film.Film",
        "namespace": "film",
        "shortDisplayName": "Film",
        "longDisplayName": "film.Film"
      },
      "attributes": {
        "film_id": {
          "type": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "shortDisplayName": "FilmId",
            "longDisplayName": "films.FilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.FilmId",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "Id",
                "parameters": [],
                "name": "Id",
                "parameterizedName": "Id",
                "namespace": "",
                "shortDisplayName": "Id",
                "longDisplayName": "Id"
              },
              "params": {}
            }
          ],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "shortDisplayName": "Title",
            "longDisplayName": "film.types.Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "description": {
          "type": {
            "fullyQualifiedName": "film.types.Description",
            "parameters": [],
            "name": "Description",
            "parameterizedName": "film.types.Description",
            "namespace": "film.types",
            "shortDisplayName": "Description",
            "longDisplayName": "film.types.Description"
          },
          "modifiers": [],
          "typeDoc": "What a film marketing person has described this film as. In space, no one hears you scream.",
          "nullable": true,
          "typeDisplayName": "film.types.Description",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "release_year": {
          "type": {
            "fullyQualifiedName": "film.types.ReleaseYear",
            "parameters": [],
            "name": "ReleaseYear",
            "parameterizedName": "film.types.ReleaseYear",
            "namespace": "film.types",
            "shortDisplayName": "ReleaseYear",
            "longDisplayName": "film.types.ReleaseYear"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "film.types.ReleaseYear",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "language_id": {
          "type": {
            "fullyQualifiedName": "language.types.LanguageId",
            "parameters": [],
            "name": "LanguageId",
            "parameterizedName": "language.types.LanguageId",
            "namespace": "language.types",
            "shortDisplayName": "LanguageId",
            "longDisplayName": "language.types.LanguageId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "language.types.LanguageId",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "original_language_id": {
          "type": {
            "fullyQualifiedName": "language.types.LanguageId",
            "parameters": [],
            "name": "LanguageId",
            "parameterizedName": "language.types.LanguageId",
            "namespace": "language.types",
            "shortDisplayName": "LanguageId",
            "longDisplayName": "language.types.LanguageId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "language.types.LanguageId",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "rental_duration": {
          "type": {
            "fullyQualifiedName": "film.types.RentalDuration",
            "parameters": [],
            "name": "RentalDuration",
            "parameterizedName": "film.types.RentalDuration",
            "namespace": "film.types",
            "shortDisplayName": "RentalDuration",
            "longDisplayName": "film.types.RentalDuration"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.RentalDuration",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "rental_rate": {
          "type": {
            "fullyQualifiedName": "film.types.RentalRate",
            "parameters": [],
            "name": "RentalRate",
            "parameterizedName": "film.types.RentalRate",
            "namespace": "film.types",
            "shortDisplayName": "RentalRate",
            "longDisplayName": "film.types.RentalRate"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.RentalRate",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "length": {
          "type": {
            "fullyQualifiedName": "film.types.Length",
            "parameters": [],
            "name": "Length",
            "parameterizedName": "film.types.Length",
            "namespace": "film.types",
            "shortDisplayName": "Length",
            "longDisplayName": "film.types.Length"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "film.types.Length",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "replacement_cost": {
          "type": {
            "fullyQualifiedName": "film.types.ReplacementCost",
            "parameters": [],
            "name": "ReplacementCost",
            "parameterizedName": "film.types.ReplacementCost",
            "namespace": "film.types",
            "shortDisplayName": "ReplacementCost",
            "longDisplayName": "film.types.ReplacementCost"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.ReplacementCost",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "rating": {
          "type": {
            "fullyQualifiedName": "film.types.Rating",
            "parameters": [],
            "name": "Rating",
            "parameterizedName": "film.types.Rating",
            "namespace": "film.types",
            "shortDisplayName": "Rating",
            "longDisplayName": "film.types.Rating"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "film.types.Rating",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "last_update": {
          "type": {
            "fullyQualifiedName": "film.types.LastUpdate",
            "parameters": [],
            "name": "LastUpdate",
            "parameterizedName": "film.types.LastUpdate",
            "namespace": "film.types",
            "shortDisplayName": "LastUpdate",
            "longDisplayName": "film.types.LastUpdate"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.LastUpdate",
          "metadata": [],
          "sourcedBy": null,
          "format": {
            "patterns": [
              "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
            ],
            "utcZoneOffsetInMinutes": null,
            "definesPattern": true,
            "isEmpty": false
          },
          "anonymousType": null
        },
        "special_features": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "film.types.SpecialFeatures",
                "parameters": [],
                "name": "SpecialFeatures",
                "parameterizedName": "film.types.SpecialFeatures",
                "namespace": "film.types",
                "shortDisplayName": "SpecialFeatures",
                "longDisplayName": "film.types.SpecialFeatures"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<film.types.SpecialFeatures>",
            "namespace": "lang.taxi",
            "shortDisplayName": "SpecialFeatures[]",
            "longDisplayName": "film.types.SpecialFeatures[]"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "film.types.SpecialFeatures[]",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "fulltext": {
          "type": {
            "fullyQualifiedName": "film.types.Fulltext",
            "parameters": [],
            "name": "Fulltext",
            "parameterizedName": "film.types.Fulltext",
            "namespace": "film.types",
            "shortDisplayName": "Fulltext",
            "longDisplayName": "film.types.Fulltext"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Fulltext",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        }
      },
      "modifiers": [],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "com.orbitalhq.jdbc.Table",
            "parameters": [],
            "name": "Table",
            "parameterizedName": "com.orbitalhq.jdbc.Table",
            "namespace": "com.orbitalhq.jdbc",
            "shortDisplayName": "Table",
            "longDisplayName": "com.orbitalhq.jdbc.Table"
          },
          "params": {
            "table": "film",
            "schema": "public",
            "connection": "films"
          }
        }
      ],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\Film.taxi",
          "version": "0.0.0",
          "content": "[[ This is a film ]]\r\n   @com.orbitalhq.jdbc.Table(table = \"film\" , schema = \"public\" , connection = \"films\")\r\n      model Film {\r\n         @Id film_id : films.FilmId\r\n         title : film.types.Title\r\n         [[ What a film marketing person has described this film as. In space, no one hears you scream. ]]\r\n         description : film.types.Description?\r\n         release_year : film.types.ReleaseYear?\r\n         language_id : language.types.LanguageId\r\n         original_language_id : language.types.LanguageId?\r\n         rental_duration : film.types.RentalDuration\r\n         rental_rate : film.types.RentalRate\r\n         length : film.types.Length?\r\n         replacement_cost : film.types.ReplacementCost\r\n         rating : film.types.Rating?\r\n         last_update : film.types.LastUpdate\r\n         special_features : film.types.SpecialFeatures[]?\r\n         fulltext : film.types.Fulltext\r\n      }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\Film.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\Film.taxi:0.0.0",
          "contentHash": "b4eff0",
          "fullHash": "471517c65c565894c890c3c9baf367e5f74dd67fd7cf1cb439265cbf7719ed3d"
        }
      ],
      "typeParameters": [],
      "typeDoc": "This is a film",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.Film",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.Film",
      "fullyQualifiedName": "film.Film",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.Film",
        "parameters": [],
        "name": "Film",
        "parameterizedName": "film.Film",
        "namespace": "film",
        "shortDisplayName": "Film",
        "longDisplayName": "film.Film"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.Film",
          "parameters": [],
          "name": "Film",
          "parameterizedName": "film.Film",
          "namespace": "film",
          "shortDisplayName": "Film",
          "longDisplayName": "film.Film"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "demo.netflix.NetflixFilmId",
        "parameters": [],
        "name": "NetflixFilmId",
        "parameterizedName": "demo.netflix.NetflixFilmId",
        "namespace": "demo.netflix",
        "shortDisplayName": "NetflixFilmId",
        "longDisplayName": "demo.netflix.NetflixFilmId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi",
          "version": "0.0.0",
          "content": "type NetflixFilmId inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi:0.0.0",
          "contentHash": "f835f0",
          "fullHash": "1c4072e105ff0dbc343829fc26a3db84b9982490c8b4d3647ba7a8e0b987c946"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "demo.netflix.NetflixFilmId",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "demo.netflix.NetflixFilmId",
      "fullyQualifiedName": "demo.netflix.NetflixFilmId",
      "memberQualifiedName": {
        "fullyQualifiedName": "demo.netflix.NetflixFilmId",
        "parameters": [],
        "name": "NetflixFilmId",
        "parameterizedName": "demo.netflix.NetflixFilmId",
        "namespace": "demo.netflix",
        "shortDisplayName": "NetflixFilmId",
        "longDisplayName": "demo.netflix.NetflixFilmId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "demo.netflix.NetflixFilmId",
          "parameters": [],
          "name": "NetflixFilmId",
          "parameterizedName": "demo.netflix.NetflixFilmId",
          "namespace": "demo.netflix",
          "shortDisplayName": "NetflixFilmId",
          "longDisplayName": "demo.netflix.NetflixFilmId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
        "parameters": [],
        "name": "NewFilmReleaseAnnouncement",
        "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
        "namespace": "demo.netflix",
        "shortDisplayName": "NewFilmReleaseAnnouncement",
        "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
      },
      "attributes": {
        "filmId": {
          "type": {
            "fullyQualifiedName": "demo.netflix.NetflixFilmId",
            "parameters": [],
            "name": "NetflixFilmId",
            "parameterizedName": "demo.netflix.NetflixFilmId",
            "namespace": "demo.netflix",
            "shortDisplayName": "NetflixFilmId",
            "longDisplayName": "demo.netflix.NetflixFilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "demo.netflix.NetflixFilmId",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "Id",
                "parameters": [],
                "name": "Id",
                "parameterizedName": "Id",
                "namespace": "",
                "shortDisplayName": "Id",
                "longDisplayName": "Id"
              },
              "params": {}
            }
          ],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "announcement": {
          "type": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": true,
          "typeDisplayName": "lang.taxi.String",
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
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi",
          "version": "0.0.0",
          "content": "model NewFilmReleaseAnnouncement {\r\n   //@lang.taxi.formats.ProtobufField(tag = 1 , protoType = \"int32\")\r\n   @Id filmId : NetflixFilmId?\r\n   //@lang.taxi.formats.ProtobufField(tag = 2 , protoType = \"string\")\r\n   announcement : String?\r\n}",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\netflix\\netflix-types.taxi:0.0.0",
          "contentHash": "08563a",
          "fullHash": "17348c1ec3edd99959ed959d1d19359cb98a21723855605bfab0d48f5673afb2"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement",
      "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
      "memberQualifiedName": {
        "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
        "parameters": [],
        "name": "NewFilmReleaseAnnouncement",
        "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
        "namespace": "demo.netflix",
        "shortDisplayName": "NewFilmReleaseAnnouncement",
        "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
          "parameters": [],
          "name": "NewFilmReleaseAnnouncement",
          "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
          "namespace": "demo.netflix",
          "shortDisplayName": "NewFilmReleaseAnnouncement",
          "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.Description",
        "parameters": [],
        "name": "Description",
        "parameterizedName": "film.types.Description",
        "namespace": "film.types",
        "shortDisplayName": "Description",
        "longDisplayName": "film.types.Description"
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
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\Description.taxi",
          "version": "0.0.0",
          "content": "[[ The description of the film ]]\r\n   type Description inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\Description.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\Description.taxi:0.0.0",
          "contentHash": "caa762",
          "fullHash": "bf73ff9240ee6b7db9299bb054dcb02f630014d2c5b1a227cf3811aea71a8200"
        }
      ],
      "typeParameters": [],
      "typeDoc": "The description of the film",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.Description",
      "constraintsByPath": [],
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
      "longDisplayName": "film.types.Description",
      "fullyQualifiedName": "film.types.Description",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.Description",
        "parameters": [],
        "name": "Description",
        "parameterizedName": "film.types.Description",
        "namespace": "film.types",
        "shortDisplayName": "Description",
        "longDisplayName": "film.types.Description"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.Description",
          "parameters": [],
          "name": "Description",
          "parameterizedName": "film.types.Description",
          "namespace": "film.types",
          "shortDisplayName": "Description",
          "longDisplayName": "film.types.Description"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.Fulltext",
        "parameters": [],
        "name": "Fulltext",
        "parameterizedName": "film.types.Fulltext",
        "namespace": "film.types",
        "shortDisplayName": "Fulltext",
        "longDisplayName": "film.types.Fulltext"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\Fulltext.taxi",
          "version": "0.0.0",
          "content": "type Fulltext inherits Any",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\Fulltext.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\Fulltext.taxi:0.0.0",
          "contentHash": "1b7004",
          "fullHash": "fff3c1f86104fba72f6dc583d15dffdaf949f78d73d9c9898a310f437b347a4d"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.Fulltext",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.Fulltext",
      "fullyQualifiedName": "film.types.Fulltext",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.Fulltext",
        "parameters": [],
        "name": "Fulltext",
        "parameterizedName": "film.types.Fulltext",
        "namespace": "film.types",
        "shortDisplayName": "Fulltext",
        "longDisplayName": "film.types.Fulltext"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.Fulltext",
          "parameters": [],
          "name": "Fulltext",
          "parameterizedName": "film.types.Fulltext",
          "namespace": "film.types",
          "shortDisplayName": "Fulltext",
          "longDisplayName": "film.types.Fulltext"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.LastUpdate",
        "parameters": [],
        "name": "LastUpdate",
        "parameterizedName": "film.types.LastUpdate",
        "namespace": "film.types",
        "shortDisplayName": "LastUpdate",
        "longDisplayName": "film.types.LastUpdate"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "name": "Instant",
          "parameterizedName": "lang.taxi.Instant",
          "namespace": "lang.taxi",
          "shortDisplayName": "Instant",
          "longDisplayName": "lang.taxi.Instant"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\LastUpdate.taxi",
          "version": "0.0.0",
          "content": "type LastUpdate inherits Instant",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\LastUpdate.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\LastUpdate.taxi:0.0.0",
          "contentHash": "1ce1bc",
          "fullHash": "dd6528034084eb1b9587798d5d0d67e2af9d5a90b2dec8b53a6f3c00b93f5fac"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.LastUpdate",
      "constraintsByPath": [],
      "isAnonymous": false,
      "isTypeAlias": false,
      "formatAndZoneOffset": {
        "patterns": [
          "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
        ],
        "utcZoneOffsetInMinutes": null,
        "definesPattern": true,
        "isEmpty": false
      },
      "offset": null,
      "format": [
        "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
      ],
      "hasFormat": true,
      "declaresFormat": true,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Instant",
        "parameters": [],
        "name": "Instant",
        "parameterizedName": "lang.taxi.Instant",
        "namespace": "lang.taxi",
        "shortDisplayName": "Instant",
        "longDisplayName": "lang.taxi.Instant"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.LastUpdate",
      "fullyQualifiedName": "film.types.LastUpdate",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.LastUpdate",
        "parameters": [],
        "name": "LastUpdate",
        "parameterizedName": "film.types.LastUpdate",
        "namespace": "film.types",
        "shortDisplayName": "LastUpdate",
        "longDisplayName": "film.types.LastUpdate"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.LastUpdate",
          "parameters": [],
          "name": "LastUpdate",
          "parameterizedName": "film.types.LastUpdate",
          "namespace": "film.types",
          "shortDisplayName": "LastUpdate",
          "longDisplayName": "film.types.LastUpdate"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.Length",
        "parameters": [],
        "name": "Length",
        "parameterizedName": "film.types.Length",
        "namespace": "film.types",
        "shortDisplayName": "Length",
        "longDisplayName": "film.types.Length"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\Length.taxi",
          "version": "0.0.0",
          "content": "type Length inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\Length.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\Length.taxi:0.0.0",
          "contentHash": "fee182",
          "fullHash": "35389a3ef6438f06bc5e6902496ff5f1f2a8684779a89ef65208e9a764f22d72"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.Length",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.Length",
      "fullyQualifiedName": "film.types.Length",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.Length",
        "parameters": [],
        "name": "Length",
        "parameterizedName": "film.types.Length",
        "namespace": "film.types",
        "shortDisplayName": "Length",
        "longDisplayName": "film.types.Length"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.Length",
          "parameters": [],
          "name": "Length",
          "parameterizedName": "film.types.Length",
          "namespace": "film.types",
          "shortDisplayName": "Length",
          "longDisplayName": "film.types.Length"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.Rating",
        "parameters": [],
        "name": "Rating",
        "parameterizedName": "film.types.Rating",
        "namespace": "film.types",
        "shortDisplayName": "Rating",
        "longDisplayName": "film.types.Rating"
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
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\Rating.taxi",
          "version": "0.0.0",
          "content": "type Rating inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\Rating.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\Rating.taxi:0.0.0",
          "contentHash": "05e62d",
          "fullHash": "03a89f80bb1fdf38c5bc2f77905e795b054691d61bf7c2f24b5db7790c1891d1"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.Rating",
      "constraintsByPath": [],
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
      "longDisplayName": "film.types.Rating",
      "fullyQualifiedName": "film.types.Rating",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.Rating",
        "parameters": [],
        "name": "Rating",
        "parameterizedName": "film.types.Rating",
        "namespace": "film.types",
        "shortDisplayName": "Rating",
        "longDisplayName": "film.types.Rating"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.Rating",
          "parameters": [],
          "name": "Rating",
          "parameterizedName": "film.types.Rating",
          "namespace": "film.types",
          "shortDisplayName": "Rating",
          "longDisplayName": "film.types.Rating"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.ReleaseYear",
        "parameters": [],
        "name": "ReleaseYear",
        "parameterizedName": "film.types.ReleaseYear",
        "namespace": "film.types",
        "shortDisplayName": "ReleaseYear",
        "longDisplayName": "film.types.ReleaseYear"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Any",
          "parameters": [],
          "name": "Any",
          "parameterizedName": "lang.taxi.Any",
          "namespace": "lang.taxi",
          "shortDisplayName": "Any",
          "longDisplayName": "lang.taxi.Any"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReleaseYear.taxi",
          "version": "0.0.0",
          "content": "type ReleaseYear inherits Any",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReleaseYear.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReleaseYear.taxi:0.0.0",
          "contentHash": "3874e9",
          "fullHash": "63460ed0eef4e7ff229c0b71996a3f576dba7d9836298beae65b0f109cc1a605"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.ReleaseYear",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.ReleaseYear",
      "fullyQualifiedName": "film.types.ReleaseYear",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.ReleaseYear",
        "parameters": [],
        "name": "ReleaseYear",
        "parameterizedName": "film.types.ReleaseYear",
        "namespace": "film.types",
        "shortDisplayName": "ReleaseYear",
        "longDisplayName": "film.types.ReleaseYear"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.ReleaseYear",
          "parameters": [],
          "name": "ReleaseYear",
          "parameterizedName": "film.types.ReleaseYear",
          "namespace": "film.types",
          "shortDisplayName": "ReleaseYear",
          "longDisplayName": "film.types.ReleaseYear"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.RentalDuration",
        "parameters": [],
        "name": "RentalDuration",
        "parameterizedName": "film.types.RentalDuration",
        "namespace": "film.types",
        "shortDisplayName": "RentalDuration",
        "longDisplayName": "film.types.RentalDuration"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalDuration.taxi",
          "version": "0.0.0",
          "content": "type RentalDuration inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalDuration.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalDuration.taxi:0.0.0",
          "contentHash": "a855d1",
          "fullHash": "d510cbd01bf2463cc6b03ae69f4e695e882c12ae74ef2cea9a9d8d328f9103f8"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.RentalDuration",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.RentalDuration",
      "fullyQualifiedName": "film.types.RentalDuration",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.RentalDuration",
        "parameters": [],
        "name": "RentalDuration",
        "parameterizedName": "film.types.RentalDuration",
        "namespace": "film.types",
        "shortDisplayName": "RentalDuration",
        "longDisplayName": "film.types.RentalDuration"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.RentalDuration",
          "parameters": [],
          "name": "RentalDuration",
          "parameterizedName": "film.types.RentalDuration",
          "namespace": "film.types",
          "shortDisplayName": "RentalDuration",
          "longDisplayName": "film.types.RentalDuration"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.RentalRate",
        "parameters": [],
        "name": "RentalRate",
        "parameterizedName": "film.types.RentalRate",
        "namespace": "film.types",
        "shortDisplayName": "RentalRate",
        "longDisplayName": "film.types.RentalRate"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "shortDisplayName": "Decimal",
          "longDisplayName": "lang.taxi.Decimal"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalRate.taxi",
          "version": "0.0.0",
          "content": "type RentalRate inherits Decimal",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalRate.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\RentalRate.taxi:0.0.0",
          "contentHash": "442274",
          "fullHash": "1ffc80808ba25e3547d56692bcce93945278810b6b592badf6b3851488ac4c2c"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.RentalRate",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Decimal",
        "parameters": [],
        "name": "Decimal",
        "parameterizedName": "lang.taxi.Decimal",
        "namespace": "lang.taxi",
        "shortDisplayName": "Decimal",
        "longDisplayName": "lang.taxi.Decimal"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.RentalRate",
      "fullyQualifiedName": "film.types.RentalRate",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.RentalRate",
        "parameters": [],
        "name": "RentalRate",
        "parameterizedName": "film.types.RentalRate",
        "namespace": "film.types",
        "shortDisplayName": "RentalRate",
        "longDisplayName": "film.types.RentalRate"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.RentalRate",
          "parameters": [],
          "name": "RentalRate",
          "parameterizedName": "film.types.RentalRate",
          "namespace": "film.types",
          "shortDisplayName": "RentalRate",
          "longDisplayName": "film.types.RentalRate"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.ReplacementCost",
        "parameters": [],
        "name": "ReplacementCost",
        "parameterizedName": "film.types.ReplacementCost",
        "namespace": "film.types",
        "shortDisplayName": "ReplacementCost",
        "longDisplayName": "film.types.ReplacementCost"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "shortDisplayName": "Decimal",
          "longDisplayName": "lang.taxi.Decimal"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReplacementCost.taxi",
          "version": "0.0.0",
          "content": "type ReplacementCost inherits Decimal",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReplacementCost.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\ReplacementCost.taxi:0.0.0",
          "contentHash": "5e092b",
          "fullHash": "458d3f143ac8a7a72e87ad0b1c9fb1bb87eb94e0eb5cf53e0fe50313c506d92b"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.ReplacementCost",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Decimal",
        "parameters": [],
        "name": "Decimal",
        "parameterizedName": "lang.taxi.Decimal",
        "namespace": "lang.taxi",
        "shortDisplayName": "Decimal",
        "longDisplayName": "lang.taxi.Decimal"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "film.types.ReplacementCost",
      "fullyQualifiedName": "film.types.ReplacementCost",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.ReplacementCost",
        "parameters": [],
        "name": "ReplacementCost",
        "parameterizedName": "film.types.ReplacementCost",
        "namespace": "film.types",
        "shortDisplayName": "ReplacementCost",
        "longDisplayName": "film.types.ReplacementCost"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.ReplacementCost",
          "parameters": [],
          "name": "ReplacementCost",
          "parameterizedName": "film.types.ReplacementCost",
          "namespace": "film.types",
          "shortDisplayName": "ReplacementCost",
          "longDisplayName": "film.types.ReplacementCost"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.SpecialFeatures",
        "parameters": [],
        "name": "SpecialFeatures",
        "parameterizedName": "film.types.SpecialFeatures",
        "namespace": "film.types",
        "shortDisplayName": "SpecialFeatures",
        "longDisplayName": "film.types.SpecialFeatures"
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
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\SpecialFeatures.taxi",
          "version": "0.0.0",
          "content": "type SpecialFeatures inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\SpecialFeatures.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\SpecialFeatures.taxi:0.0.0",
          "contentHash": "6325c9",
          "fullHash": "4ac04015edd8bf04668ad27450c59bd665b847f2cdc761e8559cc7605e6c95e3"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.SpecialFeatures",
      "constraintsByPath": [],
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
      "longDisplayName": "film.types.SpecialFeatures",
      "fullyQualifiedName": "film.types.SpecialFeatures",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.SpecialFeatures",
        "parameters": [],
        "name": "SpecialFeatures",
        "parameterizedName": "film.types.SpecialFeatures",
        "namespace": "film.types",
        "shortDisplayName": "SpecialFeatures",
        "longDisplayName": "film.types.SpecialFeatures"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.SpecialFeatures",
          "parameters": [],
          "name": "SpecialFeatures",
          "parameterizedName": "film.types.SpecialFeatures",
          "namespace": "film.types",
          "shortDisplayName": "SpecialFeatures",
          "longDisplayName": "film.types.SpecialFeatures"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "film.types.Title",
        "parameters": [],
        "name": "Title",
        "parameterizedName": "film.types.Title",
        "namespace": "film.types",
        "shortDisplayName": "Title",
        "longDisplayName": "film.types.Title"
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
          "name": "[demo.vyne/films-demo/0.1.0]/film\\types\\Title.taxi",
          "version": "0.0.0",
          "content": "[[ The title of the film ]]\r\n   type Title inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\types\\Title.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\types\\Title.taxi:0.0.0",
          "contentHash": "a61efe",
          "fullHash": "cb5d833ab731f66414712fe15af1321a95a4377f7c1f0ff396b27d76450d3f90"
        }
      ],
      "typeParameters": [],
      "typeDoc": "The title of the film",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "film.types.Title",
      "constraintsByPath": [],
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
      "longDisplayName": "film.types.Title",
      "fullyQualifiedName": "film.types.Title",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.types.Title",
        "parameters": [],
        "name": "Title",
        "parameterizedName": "film.types.Title",
        "namespace": "film.types",
        "shortDisplayName": "Title",
        "longDisplayName": "film.types.Title"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.types.Title",
          "parameters": [],
          "name": "Title",
          "parameterizedName": "film.types.Title",
          "namespace": "film.types",
          "shortDisplayName": "Title",
          "longDisplayName": "film.types.Title"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.StreamingProviderName",
        "parameters": [],
        "name": "StreamingProviderName",
        "parameterizedName": "films.StreamingProviderName",
        "namespace": "films",
        "shortDisplayName": "StreamingProviderName",
        "longDisplayName": "films.StreamingProviderName"
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
          "name": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "version": "0.0.0",
          "content": "[[ The name of a streaming service where people can watch a movie ]]\r\ntype StreamingProviderName inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/films.taxi:0.0.0",
          "contentHash": "c7e800",
          "fullHash": "b6976e44d7a590c6d2e4865320c1da98cfe60859986206d9af4c36c789c35956"
        }
      ],
      "typeParameters": [],
      "typeDoc": "The name of a streaming service where people can watch a movie",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.StreamingProviderName",
      "constraintsByPath": [],
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
      "longDisplayName": "films.StreamingProviderName",
      "fullyQualifiedName": "films.StreamingProviderName",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.StreamingProviderName",
        "parameters": [],
        "name": "StreamingProviderName",
        "parameterizedName": "films.StreamingProviderName",
        "namespace": "films",
        "shortDisplayName": "StreamingProviderName",
        "longDisplayName": "films.StreamingProviderName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.StreamingProviderName",
          "parameters": [],
          "name": "StreamingProviderName",
          "parameterizedName": "films.StreamingProviderName",
          "namespace": "films",
          "shortDisplayName": "StreamingProviderName",
          "longDisplayName": "films.StreamingProviderName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.StreamingProviderPrice",
        "parameters": [],
        "name": "StreamingProviderPrice",
        "parameterizedName": "films.StreamingProviderPrice",
        "namespace": "films",
        "shortDisplayName": "StreamingProviderPrice",
        "longDisplayName": "films.StreamingProviderPrice"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "shortDisplayName": "Decimal",
          "longDisplayName": "lang.taxi.Decimal"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "version": "0.0.0",
          "content": "[[ The monthly subscription cost of a streaming service where people can watch movies ]]\r\ntype StreamingProviderPrice inherits Decimal",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/films.taxi:0.0.0",
          "contentHash": "d521ee",
          "fullHash": "09b10900d655b4d9585eeb4ae4ab0799779e976ef970fbfc380a4c2ac9e9b7fb"
        }
      ],
      "typeParameters": [],
      "typeDoc": "The monthly subscription cost of a streaming service where people can watch movies",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.StreamingProviderPrice",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Decimal",
        "parameters": [],
        "name": "Decimal",
        "parameterizedName": "lang.taxi.Decimal",
        "namespace": "lang.taxi",
        "shortDisplayName": "Decimal",
        "longDisplayName": "lang.taxi.Decimal"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "films.StreamingProviderPrice",
      "fullyQualifiedName": "films.StreamingProviderPrice",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.StreamingProviderPrice",
        "parameters": [],
        "name": "StreamingProviderPrice",
        "parameterizedName": "films.StreamingProviderPrice",
        "namespace": "films",
        "shortDisplayName": "StreamingProviderPrice",
        "longDisplayName": "films.StreamingProviderPrice"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.StreamingProviderPrice",
          "parameters": [],
          "name": "StreamingProviderPrice",
          "parameterizedName": "films.StreamingProviderPrice",
          "namespace": "films",
          "shortDisplayName": "StreamingProviderPrice",
          "longDisplayName": "films.StreamingProviderPrice"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.FilmId",
        "parameters": [],
        "name": "FilmId",
        "parameterizedName": "films.FilmId",
        "namespace": "films",
        "shortDisplayName": "FilmId",
        "longDisplayName": "films.FilmId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "version": "0.0.0",
          "content": "type FilmId inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/films.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/films.taxi:0.0.0",
          "contentHash": "c50682",
          "fullHash": "2297ad8f797c00483d32200120a7bd1e65d1f15c3a5879fecfbf8d231357fd5b"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.FilmId",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "films.FilmId",
      "fullyQualifiedName": "films.FilmId",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.FilmId",
        "parameters": [],
        "name": "FilmId",
        "parameterizedName": "films.FilmId",
        "namespace": "films",
        "shortDisplayName": "FilmId",
        "longDisplayName": "films.FilmId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.FilmId",
          "parameters": [],
          "name": "FilmId",
          "parameterizedName": "films.FilmId",
          "namespace": "films",
          "shortDisplayName": "FilmId",
          "longDisplayName": "films.FilmId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "language.types.LanguageId",
        "parameters": [],
        "name": "LanguageId",
        "parameterizedName": "language.types.LanguageId",
        "namespace": "language.types",
        "shortDisplayName": "LanguageId",
        "longDisplayName": "language.types.LanguageId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/language\\types\\LanguageId.taxi",
          "version": "0.0.0",
          "content": "type LanguageId inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/language\\types\\LanguageId.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/language\\types\\LanguageId.taxi:0.0.0",
          "contentHash": "a6b88f",
          "fullHash": "bb5082fb2c046b01e704a47a6497204ecfc49fc7aa6e006f63ab0874661fbff5"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "language.types.LanguageId",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "language.types.LanguageId",
      "fullyQualifiedName": "language.types.LanguageId",
      "memberQualifiedName": {
        "fullyQualifiedName": "language.types.LanguageId",
        "parameters": [],
        "name": "LanguageId",
        "parameterizedName": "language.types.LanguageId",
        "namespace": "language.types",
        "shortDisplayName": "LanguageId",
        "longDisplayName": "language.types.LanguageId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "language.types.LanguageId",
          "parameters": [],
          "name": "LanguageId",
          "parameterizedName": "language.types.LanguageId",
          "namespace": "language.types",
          "shortDisplayName": "LanguageId",
          "longDisplayName": "language.types.LanguageId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
        "parameters": [],
        "name": "SquashedTomatoesFilmId",
        "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
        "namespace": "films.reviews",
        "shortDisplayName": "SquashedTomatoesFilmId",
        "longDisplayName": "films.reviews.SquashedTomatoesFilmId"
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
          "name": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "version": "0.0.0",
          "content": "type SquashedTomatoesFilmId inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi:0.0.0",
          "contentHash": "5fb5f0",
          "fullHash": "83d28ac9a4a9d0a9b8abc179d007de961b98bcf6088159dec9dbe0eda438d0a1"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.reviews.SquashedTomatoesFilmId",
      "constraintsByPath": [],
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
      "longDisplayName": "films.reviews.SquashedTomatoesFilmId",
      "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
        "parameters": [],
        "name": "SquashedTomatoesFilmId",
        "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
        "namespace": "films.reviews",
        "shortDisplayName": "SquashedTomatoesFilmId",
        "longDisplayName": "films.reviews.SquashedTomatoesFilmId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.reviews.SquashedTomatoesFilmId",
          "parameters": [],
          "name": "SquashedTomatoesFilmId",
          "parameterizedName": "films.reviews.SquashedTomatoesFilmId",
          "namespace": "films.reviews",
          "shortDisplayName": "SquashedTomatoesFilmId",
          "longDisplayName": "films.reviews.SquashedTomatoesFilmId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.reviews.FilmReviewScore",
        "parameters": [],
        "name": "FilmReviewScore",
        "parameterizedName": "films.reviews.FilmReviewScore",
        "namespace": "films.reviews",
        "shortDisplayName": "FilmReviewScore",
        "longDisplayName": "films.reviews.FilmReviewScore"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Decimal",
          "parameters": [],
          "name": "Decimal",
          "parameterizedName": "lang.taxi.Decimal",
          "namespace": "lang.taxi",
          "shortDisplayName": "Decimal",
          "longDisplayName": "lang.taxi.Decimal"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "version": "0.0.0",
          "content": "[[ A score indicating how good the movie is.  Higher scores are better]]\r\ntype FilmReviewScore inherits Decimal",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi:0.0.0",
          "contentHash": "f3b66f",
          "fullHash": "d795de6a13ee20ceaa1eb43fc69b1d341e06dd4e2ad4bcab445255fb89a2c0fd"
        }
      ],
      "typeParameters": [],
      "typeDoc": "A score indicating how good the movie is.  Higher scores are better",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.reviews.FilmReviewScore",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Decimal",
        "parameters": [],
        "name": "Decimal",
        "parameterizedName": "lang.taxi.Decimal",
        "namespace": "lang.taxi",
        "shortDisplayName": "Decimal",
        "longDisplayName": "lang.taxi.Decimal"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "films.reviews.FilmReviewScore",
      "fullyQualifiedName": "films.reviews.FilmReviewScore",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.reviews.FilmReviewScore",
        "parameters": [],
        "name": "FilmReviewScore",
        "parameterizedName": "films.reviews.FilmReviewScore",
        "namespace": "films.reviews",
        "shortDisplayName": "FilmReviewScore",
        "longDisplayName": "films.reviews.FilmReviewScore"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.reviews.FilmReviewScore",
          "parameters": [],
          "name": "FilmReviewScore",
          "parameterizedName": "films.reviews.FilmReviewScore",
          "namespace": "films.reviews",
          "shortDisplayName": "FilmReviewScore",
          "longDisplayName": "films.reviews.FilmReviewScore"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "films.reviews.ReviewText",
        "parameters": [],
        "name": "ReviewText",
        "parameterizedName": "films.reviews.ReviewText",
        "namespace": "films.reviews",
        "shortDisplayName": "ReviewText",
        "longDisplayName": "films.reviews.ReviewText"
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
          "name": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "version": "0.0.0",
          "content": "[[ The text of a film review ]]\r\ntype ReviewText inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/reviews\\review-site.taxi:0.0.0",
          "contentHash": "d7d6a5",
          "fullHash": "d5e8783aee4435cd14fd22a8a1a0c77593e4ec44d098beb6ddbcd131de4aa09a"
        }
      ],
      "typeParameters": [],
      "typeDoc": "The text of a film review",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "films.reviews.ReviewText",
      "constraintsByPath": [],
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
      "longDisplayName": "films.reviews.ReviewText",
      "fullyQualifiedName": "films.reviews.ReviewText",
      "memberQualifiedName": {
        "fullyQualifiedName": "films.reviews.ReviewText",
        "parameters": [],
        "name": "ReviewText",
        "parameterizedName": "films.reviews.ReviewText",
        "namespace": "films.reviews",
        "shortDisplayName": "ReviewText",
        "longDisplayName": "films.reviews.ReviewText"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "films.reviews.ReviewText",
          "parameters": [],
          "name": "ReviewText",
          "parameterizedName": "films.reviews.ReviewText",
          "namespace": "films.reviews",
          "shortDisplayName": "ReviewText",
          "longDisplayName": "films.reviews.ReviewText"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "staff.types.StaffId",
        "parameters": [],
        "name": "StaffId",
        "parameterizedName": "staff.types.StaffId",
        "namespace": "staff.types",
        "shortDisplayName": "StaffId",
        "longDisplayName": "staff.types.StaffId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "name": "Int",
          "parameterizedName": "lang.taxi.Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/staff\\types\\StaffId.taxi",
          "version": "0.0.0",
          "content": "type StaffId inherits Int",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/staff\\types\\StaffId.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/staff\\types\\StaffId.taxi:0.0.0",
          "contentHash": "24b805",
          "fullHash": "21d316ee769693bb64d727d89175261a479feeac40cfb45dde113deb6058896f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "staff.types.StaffId",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "name": "Int",
        "parameterizedName": "lang.taxi.Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "staff.types.StaffId",
      "fullyQualifiedName": "staff.types.StaffId",
      "memberQualifiedName": {
        "fullyQualifiedName": "staff.types.StaffId",
        "parameters": [],
        "name": "StaffId",
        "parameterizedName": "staff.types.StaffId",
        "namespace": "staff.types",
        "shortDisplayName": "StaffId",
        "longDisplayName": "staff.types.StaffId"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "staff.types.StaffId",
          "parameters": [],
          "name": "StaffId",
          "parameterizedName": "staff.types.StaffId",
          "namespace": "staff.types",
          "shortDisplayName": "StaffId",
          "longDisplayName": "staff.types.StaffId"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "OrbitalRoles",
        "parameters": [],
        "name": "OrbitalRoles",
        "parameterizedName": "OrbitalRoles",
        "namespace": "",
        "shortDisplayName": "OrbitalRoles",
        "longDisplayName": "OrbitalRoles"
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
          "name": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "version": "0.0.0",
          "content": "type OrbitalRoles inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi:0.0.0",
          "contentHash": "918e93",
          "fullHash": "de754f94d8e7af7ec54d1c489377e6d0712f4c7abd67df80c74ba4dbeb527c64"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "OrbitalRoles",
      "constraintsByPath": [],
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
      "longDisplayName": "OrbitalRoles",
      "fullyQualifiedName": "OrbitalRoles",
      "memberQualifiedName": {
        "fullyQualifiedName": "OrbitalRoles",
        "parameters": [],
        "name": "OrbitalRoles",
        "parameterizedName": "OrbitalRoles",
        "namespace": "",
        "shortDisplayName": "OrbitalRoles",
        "longDisplayName": "OrbitalRoles"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "OrbitalRoles",
          "parameters": [],
          "name": "OrbitalRoles",
          "parameterizedName": "OrbitalRoles",
          "namespace": "",
          "shortDisplayName": "OrbitalRoles",
          "longDisplayName": "OrbitalRoles"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "Email",
        "parameters": [],
        "name": "Email",
        "parameterizedName": "Email",
        "namespace": "",
        "shortDisplayName": "Email",
        "longDisplayName": "Email"
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
          "name": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "version": "0.0.0",
          "content": "type Email inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi:0.0.0",
          "contentHash": "44b5ff",
          "fullHash": "f32dfe69a9c117f970494db2442b8ced184d34fd89f8d286867bcdab1ea2b8de"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "Email",
      "constraintsByPath": [],
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
      "longDisplayName": "Email",
      "fullyQualifiedName": "Email",
      "memberQualifiedName": {
        "fullyQualifiedName": "Email",
        "parameters": [],
        "name": "Email",
        "parameterizedName": "Email",
        "namespace": "",
        "shortDisplayName": "Email",
        "longDisplayName": "Email"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "Email",
          "parameters": [],
          "name": "Email",
          "parameterizedName": "Email",
          "namespace": "",
          "shortDisplayName": "Email",
          "longDisplayName": "Email"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "UserCredentials",
        "parameters": [],
        "name": "UserCredentials",
        "parameterizedName": "UserCredentials",
        "namespace": "",
        "shortDisplayName": "UserCredentials",
        "longDisplayName": "UserCredentials"
      },
      "attributes": {
        "orbitalRoles": {
          "type": {
            "fullyQualifiedName": "OrbitalRoles",
            "parameters": [],
            "name": "OrbitalRoles",
            "parameterizedName": "OrbitalRoles",
            "namespace": "",
            "shortDisplayName": "OrbitalRoles",
            "longDisplayName": "OrbitalRoles"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "OrbitalRoles",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "email": {
          "type": {
            "fullyQualifiedName": "Email",
            "parameters": [],
            "name": "Email",
            "parameterizedName": "Email",
            "namespace": "",
            "shortDisplayName": "Email",
            "longDisplayName": "Email"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "Email",
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
          "fullyQualifiedName": "com.orbitalhq.auth.AuthClaims",
          "parameters": [],
          "name": "AuthClaims",
          "parameterizedName": "com.orbitalhq.auth.AuthClaims",
          "namespace": "com.orbitalhq.auth",
          "shortDisplayName": "AuthClaims",
          "longDisplayName": "com.orbitalhq.auth.AuthClaims"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "version": "0.0.0",
          "content": "model UserCredentials inherits com.orbitalhq.auth.AuthClaims {\n   orbitalRoles : OrbitalRoles\n   email : Email\n}",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/UserCredentials.taxi:0.0.0",
          "contentHash": "664b07",
          "fullHash": "4861178a7d7d9f6cc8101e0bcc0aa3795e6f883894c81b0fa44e99d67b29639f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "UserCredentials",
      "constraintsByPath": [],
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
        "fullyQualifiedName": "lang.taxi.Any",
        "parameters": [],
        "name": "Any",
        "parameterizedName": "lang.taxi.Any",
        "namespace": "lang.taxi",
        "shortDisplayName": "Any",
        "longDisplayName": "lang.taxi.Any"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "longDisplayName": "UserCredentials",
      "fullyQualifiedName": "UserCredentials",
      "memberQualifiedName": {
        "fullyQualifiedName": "UserCredentials",
        "parameters": [],
        "name": "UserCredentials",
        "parameterizedName": "UserCredentials",
        "namespace": "",
        "shortDisplayName": "UserCredentials",
        "longDisplayName": "UserCredentials"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "UserCredentials",
          "parameters": [],
          "name": "UserCredentials",
          "parameterizedName": "UserCredentials",
          "namespace": "",
          "shortDisplayName": "UserCredentials",
          "longDisplayName": "UserCredentials"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.kafka.ConnectionName",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.kafka.ConnectionName"
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
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "ConnectionName inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "58fe9c",
          "fullHash": "d9072cb1a0b32c625b37d502da77437f320369d9344424e014593538af2d1886"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.ConnectionName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.kafka.ConnectionName",
      "fullyQualifiedName": "com.orbitalhq.kafka.ConnectionName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.ConnectionName",
        "parameters": [],
        "name": "ConnectionName",
        "parameterizedName": "com.orbitalhq.kafka.ConnectionName",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "ConnectionName",
        "longDisplayName": "com.orbitalhq.kafka.ConnectionName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.ConnectionName",
          "parameters": [],
          "name": "ConnectionName",
          "parameterizedName": "com.orbitalhq.kafka.ConnectionName",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "ConnectionName",
          "longDisplayName": "com.orbitalhq.kafka.ConnectionName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.kafka.TopicName",
        "parameters": [],
        "name": "TopicName",
        "parameterizedName": "com.orbitalhq.kafka.TopicName",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "TopicName",
        "longDisplayName": "com.orbitalhq.kafka.TopicName"
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
          "name": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "version": "0.0.0",
          "content": "TopicName inherits String",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/vyne\\internal-types.taxi:0.0.0",
          "contentHash": "e4970d",
          "fullHash": "f913bcfc19f1d4e13afc5c6a1851450f070639745120162c0b4185250cca9ee3"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "com.orbitalhq.kafka.TopicName",
      "constraintsByPath": [],
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
      "longDisplayName": "com.orbitalhq.kafka.TopicName",
      "fullyQualifiedName": "com.orbitalhq.kafka.TopicName",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.kafka.TopicName",
        "parameters": [],
        "name": "TopicName",
        "parameterizedName": "com.orbitalhq.kafka.TopicName",
        "namespace": "com.orbitalhq.kafka",
        "shortDisplayName": "TopicName",
        "longDisplayName": "com.orbitalhq.kafka.TopicName"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.kafka.TopicName",
          "parameters": [],
          "name": "TopicName",
          "parameterizedName": "com.orbitalhq.kafka.TopicName",
          "namespace": "com.orbitalhq.kafka",
          "shortDisplayName": "TopicName",
          "longDisplayName": "com.orbitalhq.kafka.TopicName"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "parameters": [],
        "name": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "parameterizedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "longDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU"
      },
      "attributes": {
        "id": {
          "type": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "shortDisplayName": "FilmId",
            "longDisplayName": "films.FilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.FilmId",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "shortDisplayName": "Title",
            "longDisplayName": "film.types.Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "reviewScore": {
          "type": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "shortDisplayName": "FilmReviewScore",
            "longDisplayName": "films.reviews.FilmReviewScore"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.FilmReviewScore",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "review": {
          "type": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "shortDisplayName": "ReviewText",
            "longDisplayName": "films.reviews.ReviewText"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.ReviewText",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "name": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderName",
            "parameters": [],
            "name": "StreamingProviderName",
            "parameterizedName": "films.StreamingProviderName",
            "namespace": "films",
            "shortDisplayName": "StreamingProviderName",
            "longDisplayName": "films.StreamingProviderName"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderName",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "price": {
          "type": {
            "fullyQualifiedName": "films.StreamingProviderPrice",
            "parameters": [],
            "name": "StreamingProviderPrice",
            "parameterizedName": "films.StreamingProviderPrice",
            "namespace": "films",
            "shortDisplayName": "StreamingProviderPrice",
            "longDisplayName": "films.StreamingProviderPrice"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.StreamingProviderPrice",
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
          "name": "[demo.vyne/films-demo/0.1.0]/Test.taxi",
          "version": "0.0.0",
          "content": "{\n      id : FilmId\n      title : Title\n      reviewScore: FilmReviewScore\n      review: ReviewText\n      // where can I watch this?\n      name: StreamingProviderName\n      price: StreamingProviderPrice\n   }[]",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/Test.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/Test.taxi:0.0.0",
          "contentHash": "2b5105",
          "fullHash": "62146638407772ff2c7fcb845c8a8a28b64467bc82547c8245bdff7800cf600d"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
      "constraintsByPath": [],
      "isAnonymous": true,
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
      "longDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
      "fullyQualifiedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "parameters": [],
        "name": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "parameterizedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
        "longDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
          "parameters": [],
          "name": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
          "parameterizedName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
          "namespace": "",
          "shortDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU",
          "longDisplayName": "AnonymousTypeB8NOkmMOJWegKunkd4FkNa9IUNU"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "parameters": [],
        "name": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "parameterizedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "longDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98"
      },
      "attributes": {
        "hello": {
          "type": {
            "fullyQualifiedName": "lang.taxi.Int",
            "parameters": [],
            "name": "Int",
            "parameterizedName": "lang.taxi.Int",
            "namespace": "lang.taxi",
            "shortDisplayName": "Int",
            "longDisplayName": "lang.taxi.Int"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "lang.taxi.Int",
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
          "name": "[demo.vyne/films-demo/0.1.0]/TestCountQuery.taxi",
          "version": "0.0.0",
          "content": "{\n       hello : 32 - 26\n   }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/TestCountQuery.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/TestCountQuery.taxi:0.0.0",
          "contentHash": "553bbd",
          "fullHash": "d1a5dd5225253aaf5464860b4557f533cc46d50c5603bacd1ccc2309b44bf115"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
      "constraintsByPath": [],
      "isAnonymous": true,
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
      "longDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
      "fullyQualifiedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "parameters": [],
        "name": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "parameterizedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
        "longDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
          "parameters": [],
          "name": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
          "parameterizedName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
          "namespace": "",
          "shortDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98",
          "longDisplayName": "AnonymousTypeYobuZW4OlACowJtu_eczRs8Am98"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "parameters": [],
        "name": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "parameterizedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "namespace": "",
        "shortDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "longDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc"
      },
      "attributes": {
        "filmId": {
          "type": {
            "fullyQualifiedName": "films.FilmId",
            "parameters": [],
            "name": "FilmId",
            "parameterizedName": "films.FilmId",
            "namespace": "films",
            "shortDisplayName": "FilmId",
            "longDisplayName": "films.FilmId"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.FilmId",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "shortDisplayName": "Title",
            "longDisplayName": "film.types.Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "review": {
          "type": {
            "fullyQualifiedName": "films.reviews.ReviewText",
            "parameters": [],
            "name": "ReviewText",
            "parameterizedName": "films.reviews.ReviewText",
            "namespace": "films.reviews",
            "shortDisplayName": "ReviewText",
            "longDisplayName": "films.reviews.ReviewText"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.ReviewText",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "duration": {
          "type": {
            "fullyQualifiedName": "film.types.RentalDuration",
            "parameters": [],
            "name": "RentalDuration",
            "parameterizedName": "film.types.RentalDuration",
            "namespace": "film.types",
            "shortDisplayName": "RentalDuration",
            "longDisplayName": "film.types.RentalDuration"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.RentalDuration",
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
          "name": "[demo.vyne/films-demo/0.1.0]/TestQ.taxi",
          "version": "0.0.0",
          "content": "{\n     filmId: FilmId\n     title: Title\n     review: ReviewText\n     duration: RentalDuration\n   }[]",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/TestQ.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/TestQ.taxi:0.0.0",
          "contentHash": "75e37f",
          "fullHash": "93e4da1363c493093d62b427136de04f14a32b2b89f6af8e1fa1566c21db11b6"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
      "constraintsByPath": [],
      "isAnonymous": true,
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
      "longDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
      "fullyQualifiedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "parameters": [],
        "name": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "parameterizedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "namespace": "",
        "shortDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
        "longDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
          "parameters": [],
          "name": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
          "parameterizedName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
          "namespace": "",
          "shortDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc",
          "longDisplayName": "AnonymousType0sUzMBdc2az2Tv2nhHOdvG4Umcc"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "parameters": [],
        "name": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "parameterizedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "longDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg"
      },
      "attributes": {
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "shortDisplayName": "Title",
            "longDisplayName": "film.types.Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "releaseDate": {
          "type": {
            "fullyQualifiedName": "film.types.ReleaseYear",
            "parameters": [],
            "name": "ReleaseYear",
            "parameterizedName": "film.types.ReleaseYear",
            "namespace": "film.types",
            "shortDisplayName": "ReleaseYear",
            "longDisplayName": "film.types.ReleaseYear"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.ReleaseYear",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "rating": {
          "type": {
            "fullyQualifiedName": "film.types.Rating",
            "parameters": [],
            "name": "Rating",
            "parameterizedName": "film.types.Rating",
            "namespace": "film.types",
            "shortDisplayName": "Rating",
            "longDisplayName": "film.types.Rating"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Rating",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "score": {
          "type": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "shortDisplayName": "FilmReviewScore",
            "longDisplayName": "films.reviews.FilmReviewScore"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.FilmReviewScore",
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
          "name": "[demo.vyne/films-demo/0.1.0]/TestQuery2.taxi",
          "version": "0.0.0",
          "content": "{\n     title : Title\n     releaseDate : ReleaseYear\n     rating : Rating\n     score : FilmReviewScore\n   }[]",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/TestQuery2.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/TestQuery2.taxi:0.0.0",
          "contentHash": "d13153",
          "fullHash": "0b3c16e36bbf6beb7f97843b253b53d6a389d788f131b24119356a91c658cad1"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
      "constraintsByPath": [],
      "isAnonymous": true,
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
      "longDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
      "fullyQualifiedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "parameters": [],
        "name": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "parameterizedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
        "longDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
          "parameters": [],
          "name": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
          "parameterizedName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
          "namespace": "",
          "shortDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg",
          "longDisplayName": "AnonymousTypeF5nibVCvHaomjDiBClxsarkEXxg"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "parameters": [],
        "name": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "parameterizedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "longDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c"
      },
      "attributes": {
        "title": {
          "type": {
            "fullyQualifiedName": "film.types.Title",
            "parameters": [],
            "name": "Title",
            "parameterizedName": "film.types.Title",
            "namespace": "film.types",
            "shortDisplayName": "Title",
            "longDisplayName": "film.types.Title"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Title",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "duration": {
          "type": {
            "fullyQualifiedName": "film.types.Length",
            "parameters": [],
            "name": "Length",
            "parameterizedName": "film.types.Length",
            "namespace": "film.types",
            "shortDisplayName": "Length",
            "longDisplayName": "film.types.Length"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "film.types.Length",
          "metadata": [],
          "sourcedBy": null,
          "format": null,
          "anonymousType": null
        },
        "reviewScore": {
          "type": {
            "fullyQualifiedName": "films.reviews.FilmReviewScore",
            "parameters": [],
            "name": "FilmReviewScore",
            "parameterizedName": "films.reviews.FilmReviewScore",
            "namespace": "films.reviews",
            "shortDisplayName": "FilmReviewScore",
            "longDisplayName": "films.reviews.FilmReviewScore"
          },
          "modifiers": [],
          "typeDoc": null,
          "nullable": false,
          "typeDisplayName": "films.reviews.FilmReviewScore",
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
          "name": "[demo.vyne/films-demo/0.1.0]/TestQuery3.taxi",
          "version": "0.0.0",
          "content": "{\n     title : Title\n     duration : Length\n     reviewScore : FilmReviewScore\n   }[]",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/TestQuery3.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/TestQuery3.taxi:0.0.0",
          "contentHash": "17e75f",
          "fullHash": "52dbe4c95b98e52a24d4d518d44c310bc28d81d3bcca17ccdc965a28aa27e1c7"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "schemaMemberKind": "TYPE",
      "paramaterizedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
      "constraintsByPath": [],
      "isAnonymous": true,
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
      "longDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
      "fullyQualifiedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
      "memberQualifiedName": {
        "fullyQualifiedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "parameters": [],
        "name": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "parameterizedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "namespace": "",
        "shortDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
        "longDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
          "parameters": [],
          "name": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
          "parameterizedName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
          "namespace": "",
          "shortDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c",
          "longDisplayName": "AnonymousTypeQ_1Iu1y_OkDrSbG97CjtK0S-t6c"
        },
        "kind": "TYPE"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    }
  ],
  "services": [
    {
      "name": {
        "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher",
        "parameters": [],
        "name": "KafkaPublisher",
        "parameterizedName": "io.vyne.demos.films.KafkaPublisher",
        "namespace": "io.vyne.demos.films",
        "shortDisplayName": "KafkaPublisher",
        "longDisplayName": "io.vyne.demos.films.KafkaPublisher"
      },
      "operations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "parameters": [],
            "name": "KafkaPublisher@@publish",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publish",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "name": "content",
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "taxi.http.RequestBody",
                    "parameters": [],
                    "name": "RequestBody",
                    "parameterizedName": "taxi.http.RequestBody",
                    "namespace": "taxi.http",
                    "shortDisplayName": "RequestBody",
                    "longDisplayName": "taxi.http.RequestBody"
                  },
                  "params": {}
                }
              ],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              }
            },
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "name": "topic",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981/kafka/{lang.taxi.String}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "parameters": [],
              "name": "KafkaRecordMetadata",
              "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "KafkaRecordMetadata",
              "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "name": "publish",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
              "parameters": [],
              "name": "KafkaPublisher@@publish",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "publish",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "parameters": [],
            "name": "KafkaPublisher@@publish",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publish",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@startAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "startAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/announcements/start"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "kotlin.Unit",
              "parameters": [],
              "name": "Unit",
              "parameterizedName": "kotlin.Unit",
              "namespace": "kotlin",
              "shortDisplayName": "Unit",
              "longDisplayName": "kotlin.Unit"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "name": "startAutoPublishing",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
              "parameters": [],
              "name": "KafkaPublisher@@startAutoPublishing",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "startAutoPublishing",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@startAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "startAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameters": [],
            "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publishNewReleaseAnnouncement",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.Int",
                "parameters": [],
                "name": "Int",
                "parameterizedName": "lang.taxi.Int",
                "namespace": "lang.taxi",
                "shortDisplayName": "Int",
                "longDisplayName": "lang.taxi.Int"
              },
              "name": "filmId",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.Int",
                "parameters": [],
                "name": "Int",
                "parameterizedName": "lang.taxi.Int",
                "namespace": "lang.taxi",
                "shortDisplayName": "Int",
                "longDisplayName": "lang.taxi.Int"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981/kafka/newReleases/{lang.taxi.Int}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "parameters": [],
              "name": "KafkaRecordMetadata",
              "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "KafkaRecordMetadata",
              "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "name": "publishNewReleaseAnnouncement",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
              "parameters": [],
              "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "publishNewReleaseAnnouncement",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameters": [],
            "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publishNewReleaseAnnouncement",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "parameters": [],
            "name": "KafkaPublisher@@getProtoSpec",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "getProtoSpec",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/proto"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "shortDisplayName": "String",
              "longDisplayName": "lang.taxi.String"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "name": "getProtoSpec",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
              "parameters": [],
              "name": "KafkaPublisher@@getProtoSpec",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "getProtoSpec",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "parameters": [],
            "name": "KafkaPublisher@@getProtoSpec",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "getProtoSpec",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@stopAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "stopAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/announcements/stop"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "kotlin.Unit",
              "parameters": [],
              "name": "Unit",
              "parameterizedName": "kotlin.Unit",
              "namespace": "kotlin",
              "shortDisplayName": "Unit",
              "longDisplayName": "kotlin.Unit"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "name": "stopAutoPublishing",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
              "parameters": [],
              "name": "KafkaPublisher@@stopAutoPublishing",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "stopAutoPublishing",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@stopAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "stopAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
          }
        }
      ],
      "queryOperations": [],
      "streamOperations": [],
      "tableOperations": [],
      "metadata": [],
      "sourceCode": [
        {
          "name": "[io.petflix.demos/films-service/0.0.0]/films-service",
          "version": "0.0.0",
          "content": "service KafkaPublisher {\n      @taxi.http.HttpOperation(method = \"POST\" , url = \"http://localhost:9981/kafka/{lang.taxi.String}\")\n      operation publish( @taxi.http.RequestBody content : String,  topic : String ) : KafkaRecordMetadata\n      @taxi.http.HttpOperation(method = \"GET\" , url = \"http://localhost:9981/announcements/start\")\n      operation startAutoPublishing(  ) : kotlin.Unit\n      @taxi.http.HttpOperation(method = \"POST\" , url = \"http://localhost:9981/kafka/newReleases/{lang.taxi.Int}\")\n      operation publishNewReleaseAnnouncement(  filmId : Int ) : KafkaRecordMetadata\n      @taxi.http.HttpOperation(method = \"GET\" , url = \"http://localhost:9981/proto\")\n      operation getProtoSpec(  ) : String\n      @taxi.http.HttpOperation(method = \"GET\" , url = \"http://localhost:9981/announcements/stop\")\n      operation stopAutoPublishing(  ) : kotlin.Unit\n   }",
          "packageIdentifier": {
            "organisation": "io.petflix.demos",
            "name": "films-service",
            "version": "0.0.0",
            "unversionedId": "io.petflix.demos/films-service",
            "id": "io.petflix.demos/films-service/0.0.0",
            "uriSafeId": "io.petflix.demos:films-service:0.0.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[io.petflix.demos/films-service/0.0.0]/films-service",
          "id": "[io.petflix.demos/films-service/0.0.0]/films-service:0.0.0",
          "contentHash": "decb8c",
          "fullHash": "d00cdfcdf58c6fbb31e7d4d5a334756057906822223dbf03a5d63ecaf9f73d06"
        }
      ],
      "typeDoc": null,
      "lineage": null,
      "serviceKind": "API",
      "schemaMemberKind": "SERVICE",
      "remoteOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "parameters": [],
            "name": "KafkaPublisher@@publish",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publish",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "name": "content",
              "metadata": [
                {
                  "name": {
                    "fullyQualifiedName": "taxi.http.RequestBody",
                    "parameters": [],
                    "name": "RequestBody",
                    "parameterizedName": "taxi.http.RequestBody",
                    "namespace": "taxi.http",
                    "shortDisplayName": "RequestBody",
                    "longDisplayName": "taxi.http.RequestBody"
                  },
                  "params": {}
                }
              ],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              }
            },
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              },
              "name": "topic",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.String",
                "parameters": [],
                "name": "String",
                "parameterizedName": "lang.taxi.String",
                "namespace": "lang.taxi",
                "shortDisplayName": "String",
                "longDisplayName": "lang.taxi.String"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981/kafka/{lang.taxi.String}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "parameters": [],
              "name": "KafkaRecordMetadata",
              "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "KafkaRecordMetadata",
              "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "name": "publish",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
              "parameters": [],
              "name": "KafkaPublisher@@publish",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "publish",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "parameters": [],
            "name": "KafkaPublisher@@publish",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publish",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publish",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publish"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@startAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "startAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/announcements/start"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "kotlin.Unit",
              "parameters": [],
              "name": "Unit",
              "parameterizedName": "kotlin.Unit",
              "namespace": "kotlin",
              "shortDisplayName": "Unit",
              "longDisplayName": "kotlin.Unit"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "name": "startAutoPublishing",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
              "parameters": [],
              "name": "KafkaPublisher@@startAutoPublishing",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "startAutoPublishing",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@startAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@startAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "startAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / startAutoPublishing"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameters": [],
            "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publishNewReleaseAnnouncement",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "lang.taxi.Int",
                "parameters": [],
                "name": "Int",
                "parameterizedName": "lang.taxi.Int",
                "namespace": "lang.taxi",
                "shortDisplayName": "Int",
                "longDisplayName": "lang.taxi.Int"
              },
              "name": "filmId",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "lang.taxi.Int",
                "parameters": [],
                "name": "Int",
                "parameterizedName": "lang.taxi.Int",
                "namespace": "lang.taxi",
                "shortDisplayName": "Int",
                "longDisplayName": "lang.taxi.Int"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "POST",
                "url": "http://localhost:9981/kafka/newReleases/{lang.taxi.Int}"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "parameters": [],
              "name": "KafkaRecordMetadata",
              "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "KafkaRecordMetadata",
              "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "parameters": [],
            "name": "KafkaRecordMetadata",
            "parameterizedName": "io.vyne.demos.films.KafkaRecordMetadata",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "KafkaRecordMetadata",
            "longDisplayName": "io.vyne.demos.films.KafkaRecordMetadata"
          },
          "name": "publishNewReleaseAnnouncement",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
              "parameters": [],
              "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "publishNewReleaseAnnouncement",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameters": [],
            "name": "KafkaPublisher@@publishNewReleaseAnnouncement",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@publishNewReleaseAnnouncement",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "publishNewReleaseAnnouncement",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / publishNewReleaseAnnouncement"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "parameters": [],
            "name": "KafkaPublisher@@getProtoSpec",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "getProtoSpec",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/proto"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.String",
              "parameters": [],
              "name": "String",
              "parameterizedName": "lang.taxi.String",
              "namespace": "lang.taxi",
              "shortDisplayName": "String",
              "longDisplayName": "lang.taxi.String"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.String",
            "parameters": [],
            "name": "String",
            "parameterizedName": "lang.taxi.String",
            "namespace": "lang.taxi",
            "shortDisplayName": "String",
            "longDisplayName": "lang.taxi.String"
          },
          "name": "getProtoSpec",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
              "parameters": [],
              "name": "KafkaPublisher@@getProtoSpec",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "getProtoSpec",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "parameters": [],
            "name": "KafkaPublisher@@getProtoSpec",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@getProtoSpec",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "getProtoSpec",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / getProtoSpec"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@stopAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "stopAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
          },
          "parameters": [],
          "returnType": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "operationType": "READ_ONLY",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "taxi.http.HttpOperation",
                "parameters": [],
                "name": "HttpOperation",
                "parameterizedName": "taxi.http.HttpOperation",
                "namespace": "taxi.http",
                "shortDisplayName": "HttpOperation",
                "longDisplayName": "taxi.http.HttpOperation"
              },
              "params": {
                "method": "GET",
                "url": "http://localhost:9981/announcements/stop"
              }
            }
          ],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "kotlin.Unit",
              "parameters": [],
              "name": "Unit",
              "parameterizedName": "kotlin.Unit",
              "namespace": "kotlin",
              "shortDisplayName": "Unit",
              "longDisplayName": "kotlin.Unit"
            },
            "constraints": []
          },
          "typeDoc": null,
          "operationKind": "ApiCall",
          "schemaMemberKind": "OPERATION",
          "returnTypeName": {
            "fullyQualifiedName": "kotlin.Unit",
            "parameters": [],
            "name": "Unit",
            "parameterizedName": "kotlin.Unit",
            "namespace": "kotlin",
            "shortDisplayName": "Unit",
            "longDisplayName": "kotlin.Unit"
          },
          "name": "stopAutoPublishing",
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
              "parameters": [],
              "name": "KafkaPublisher@@stopAutoPublishing",
              "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
              "namespace": "io.vyne.demos.films",
              "shortDisplayName": "stopAutoPublishing",
              "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
            },
            "kind": "OPERATION"
          },
          "memberQualifiedName": {
            "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "parameters": [],
            "name": "KafkaPublisher@@stopAutoPublishing",
            "parameterizedName": "io.vyne.demos.films.KafkaPublisher@@stopAutoPublishing",
            "namespace": "io.vyne.demos.films",
            "shortDisplayName": "stopAutoPublishing",
            "longDisplayName": "io.vyne.demos.films.KafkaPublisher / stopAutoPublishing"
          }
        }
      ],
      "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher",
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher",
          "parameters": [],
          "name": "KafkaPublisher",
          "parameterizedName": "io.vyne.demos.films.KafkaPublisher",
          "namespace": "io.vyne.demos.films",
          "shortDisplayName": "KafkaPublisher",
          "longDisplayName": "io.vyne.demos.films.KafkaPublisher"
        },
        "kind": "SERVICE"
      },
      "memberQualifiedName": {
        "fullyQualifiedName": "io.vyne.demos.films.KafkaPublisher",
        "parameters": [],
        "name": "KafkaPublisher",
        "parameterizedName": "io.vyne.demos.films.KafkaPublisher",
        "namespace": "io.vyne.demos.films",
        "shortDisplayName": "KafkaPublisher",
        "longDisplayName": "io.vyne.demos.films.KafkaPublisher"
      }
    },
    {
      "name": {
        "fullyQualifiedName": "film.FilmDatabase",
        "parameters": [],
        "name": "FilmDatabase",
        "parameterizedName": "film.FilmDatabase",
        "namespace": "film",
        "shortDisplayName": "FilmDatabase",
        "longDisplayName": "film.FilmDatabase"
      },
      "operations": [],
      "queryOperations": [],
      "streamOperations": [],
      "tableOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films",
            "parameters": [],
            "name": "FilmDatabase@@films",
            "parameterizedName": "film.FilmDatabase@@films",
            "namespace": "film",
            "shortDisplayName": "films",
            "longDisplayName": "film.FilmDatabase / films"
          },
          "returnType": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<film.Film>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Film[]",
            "longDisplayName": "film.Film[]"
          },
          "metadata": [],
          "typeDoc": null,
          "queryOperations": [
            {
              "qualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
                "parameters": [],
                "name": "FilmDatabase@@films_findOneFilm",
                "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
                "namespace": "film",
                "shortDisplayName": "films_findOneFilm",
                "longDisplayName": "film.FilmDatabase / films_findOneFilm"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "shortDisplayName": "VyneQlQuery",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery"
                  },
                  "name": "body",
                  "metadata": [],
                  "constraints": [],
                  "typeDoc": null,
                  "nullable": false,
                  "defaultValue": null,
                  "allConstraints": {},
                  "typeName": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "shortDisplayName": "VyneQlQuery",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              },
              "metadata": [],
              "grammar": "vyneQl",
              "capabilities": [
                "SUM",
                "COUNT",
                "AVG",
                "MIN",
                "MAX",
                {
                  "supportedOperations": [
                    "EQUAL",
                    "NOT_EQUAL",
                    "IN",
                    "LIKE",
                    "GREATER_THAN",
                    "LESS_THAN",
                    "GREATER_THAN_OR_EQUAL_TO",
                    "LESS_THAN_OR_EQUAL_TO"
                  ]
                }
              ],
              "typeDoc": null,
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "shortDisplayName": "Film",
                  "longDisplayName": "film.Film"
                },
                "constraints": []
              },
              "operationType": "READ_ONLY",
              "hasFilterCapability": true,
              "supportedFilterOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ],
              "returnTypeName": {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              },
              "schemaMemberKind": "OPERATION",
              "operationKind": "Query",
              "name": "films_findOneFilm",
              "memberQualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
                "parameters": [],
                "name": "FilmDatabase@@films_findOneFilm",
                "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
                "namespace": "film",
                "shortDisplayName": "films_findOneFilm",
                "longDisplayName": "film.FilmDatabase / films_findOneFilm"
              },
              "schemaMemberReference": {
                "qualifiedName": {
                  "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
                  "parameters": [],
                  "name": "FilmDatabase@@films_findOneFilm",
                  "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
                  "namespace": "film",
                  "shortDisplayName": "films_findOneFilm",
                  "longDisplayName": "film.FilmDatabase / films_findOneFilm"
                },
                "kind": "OPERATION"
              }
            },
            {
              "qualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
                "parameters": [],
                "name": "FilmDatabase@@films_findManyFilm",
                "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
                "namespace": "film",
                "shortDisplayName": "films_findManyFilm",
                "longDisplayName": "film.FilmDatabase / films_findManyFilm"
              },
              "parameters": [
                {
                  "type": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "shortDisplayName": "VyneQlQuery",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery"
                  },
                  "name": "body",
                  "metadata": [],
                  "constraints": [],
                  "typeDoc": null,
                  "nullable": false,
                  "defaultValue": null,
                  "allConstraints": {},
                  "typeName": {
                    "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                    "parameters": [],
                    "name": "VyneQlQuery",
                    "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                    "namespace": "vyne.vyneQl",
                    "shortDisplayName": "VyneQlQuery",
                    "longDisplayName": "vyne.vyneQl.VyneQlQuery"
                  }
                }
              ],
              "returnType": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "shortDisplayName": "Film",
                    "longDisplayName": "film.Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "shortDisplayName": "Film[]",
                "longDisplayName": "film.Film[]"
              },
              "metadata": [],
              "grammar": "vyneQl",
              "capabilities": [
                "SUM",
                "COUNT",
                "AVG",
                "MIN",
                "MAX",
                {
                  "supportedOperations": [
                    "EQUAL",
                    "NOT_EQUAL",
                    "IN",
                    "LIKE",
                    "GREATER_THAN",
                    "LESS_THAN",
                    "GREATER_THAN_OR_EQUAL_TO",
                    "LESS_THAN_OR_EQUAL_TO"
                  ]
                }
              ],
              "typeDoc": null,
              "contract": {
                "returnType": {
                  "fullyQualifiedName": "lang.taxi.Array",
                  "parameters": [
                    {
                      "fullyQualifiedName": "film.Film",
                      "parameters": [],
                      "name": "Film",
                      "parameterizedName": "film.Film",
                      "namespace": "film",
                      "shortDisplayName": "Film",
                      "longDisplayName": "film.Film"
                    }
                  ],
                  "name": "Array",
                  "parameterizedName": "lang.taxi.Array<film.Film>",
                  "namespace": "lang.taxi",
                  "shortDisplayName": "Film[]",
                  "longDisplayName": "film.Film[]"
                },
                "constraints": []
              },
              "operationType": "READ_ONLY",
              "hasFilterCapability": true,
              "supportedFilterOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ],
              "returnTypeName": {
                "fullyQualifiedName": "lang.taxi.Array",
                "parameters": [
                  {
                    "fullyQualifiedName": "film.Film",
                    "parameters": [],
                    "name": "Film",
                    "parameterizedName": "film.Film",
                    "namespace": "film",
                    "shortDisplayName": "Film",
                    "longDisplayName": "film.Film"
                  }
                ],
                "name": "Array",
                "parameterizedName": "lang.taxi.Array<film.Film>",
                "namespace": "lang.taxi",
                "shortDisplayName": "Film[]",
                "longDisplayName": "film.Film[]"
              },
              "schemaMemberKind": "OPERATION",
              "operationKind": "Query",
              "name": "films_findManyFilm",
              "memberQualifiedName": {
                "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
                "parameters": [],
                "name": "FilmDatabase@@films_findManyFilm",
                "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
                "namespace": "film",
                "shortDisplayName": "films_findManyFilm",
                "longDisplayName": "film.FilmDatabase / films_findManyFilm"
              },
              "schemaMemberReference": {
                "qualifiedName": {
                  "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
                  "parameters": [],
                  "name": "FilmDatabase@@films_findManyFilm",
                  "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
                  "namespace": "film",
                  "shortDisplayName": "films_findManyFilm",
                  "longDisplayName": "film.FilmDatabase / films_findManyFilm"
                },
                "kind": "OPERATION"
              }
            }
          ],
          "parameters": [],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "shortDisplayName": "Film",
                  "longDisplayName": "film.Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "shortDisplayName": "Film[]",
              "longDisplayName": "film.Film[]"
            },
            "constraints": []
          },
          "operationType": "READ_ONLY",
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<film.Film>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Film[]",
            "longDisplayName": "film.Film[]"
          },
          "schemaMemberKind": "OPERATION",
          "operationKind": "Table",
          "name": "films",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films",
            "parameters": [],
            "name": "FilmDatabase@@films",
            "parameterizedName": "film.FilmDatabase@@films",
            "namespace": "film",
            "shortDisplayName": "films",
            "longDisplayName": "film.FilmDatabase / films"
          },
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films",
              "parameters": [],
              "name": "FilmDatabase@@films",
              "parameterizedName": "film.FilmDatabase@@films",
              "namespace": "film",
              "shortDisplayName": "films",
              "longDisplayName": "film.FilmDatabase / films"
            },
            "kind": "OPERATION"
          }
        }
      ],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "com.orbitalhq.jdbc.DatabaseService",
            "parameters": [],
            "name": "DatabaseService",
            "parameterizedName": "com.orbitalhq.jdbc.DatabaseService",
            "namespace": "com.orbitalhq.jdbc",
            "shortDisplayName": "DatabaseService",
            "longDisplayName": "com.orbitalhq.jdbc.DatabaseService"
          },
          "params": {
            "connection": "films"
          }
        }
      ],
      "sourceCode": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/film\\FilmService.taxi",
          "version": "0.0.0",
          "content": "@com.orbitalhq.jdbc.DatabaseService(connection = \"films\")\r\n      service FilmDatabase {\r\n         table films : Film[]\r\n      }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/film\\FilmService.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/film\\FilmService.taxi:0.0.0",
          "contentHash": "0b1b12",
          "fullHash": "eab5304011003016656e63f5d3a54e71712b6b51d79dc0b8c63094665065f0b9"
        }
      ],
      "typeDoc": null,
      "lineage": null,
      "serviceKind": "Database",
      "schemaMemberKind": "SERVICE",
      "remoteOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
            "parameters": [],
            "name": "FilmDatabase@@films_findOneFilm",
            "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
            "namespace": "film",
            "shortDisplayName": "films_findOneFilm",
            "longDisplayName": "film.FilmDatabase / films_findOneFilm"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                "parameters": [],
                "name": "VyneQlQuery",
                "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                "namespace": "vyne.vyneQl",
                "shortDisplayName": "VyneQlQuery",
                "longDisplayName": "vyne.vyneQl.VyneQlQuery"
              },
              "name": "body",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                "parameters": [],
                "name": "VyneQlQuery",
                "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                "namespace": "vyne.vyneQl",
                "shortDisplayName": "VyneQlQuery",
                "longDisplayName": "vyne.vyneQl.VyneQlQuery"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "film.Film",
            "parameters": [],
            "name": "Film",
            "parameterizedName": "film.Film",
            "namespace": "film",
            "shortDisplayName": "Film",
            "longDisplayName": "film.Film"
          },
          "metadata": [],
          "grammar": "vyneQl",
          "capabilities": [
            "SUM",
            "COUNT",
            "AVG",
            "MIN",
            "MAX",
            {
              "supportedOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ]
            }
          ],
          "typeDoc": null,
          "contract": {
            "returnType": {
              "fullyQualifiedName": "film.Film",
              "parameters": [],
              "name": "Film",
              "parameterizedName": "film.Film",
              "namespace": "film",
              "shortDisplayName": "Film",
              "longDisplayName": "film.Film"
            },
            "constraints": []
          },
          "operationType": "READ_ONLY",
          "hasFilterCapability": true,
          "supportedFilterOperations": [
            "EQUAL",
            "NOT_EQUAL",
            "IN",
            "LIKE",
            "GREATER_THAN",
            "LESS_THAN",
            "GREATER_THAN_OR_EQUAL_TO",
            "LESS_THAN_OR_EQUAL_TO"
          ],
          "returnTypeName": {
            "fullyQualifiedName": "film.Film",
            "parameters": [],
            "name": "Film",
            "parameterizedName": "film.Film",
            "namespace": "film",
            "shortDisplayName": "Film",
            "longDisplayName": "film.Film"
          },
          "schemaMemberKind": "OPERATION",
          "operationKind": "Query",
          "name": "films_findOneFilm",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
            "parameters": [],
            "name": "FilmDatabase@@films_findOneFilm",
            "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
            "namespace": "film",
            "shortDisplayName": "films_findOneFilm",
            "longDisplayName": "film.FilmDatabase / films_findOneFilm"
          },
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_findOneFilm",
              "parameters": [],
              "name": "FilmDatabase@@films_findOneFilm",
              "parameterizedName": "film.FilmDatabase@@films_findOneFilm",
              "namespace": "film",
              "shortDisplayName": "films_findOneFilm",
              "longDisplayName": "film.FilmDatabase / films_findOneFilm"
            },
            "kind": "OPERATION"
          }
        },
        {
          "qualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
            "parameters": [],
            "name": "FilmDatabase@@films_findManyFilm",
            "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
            "namespace": "film",
            "shortDisplayName": "films_findManyFilm",
            "longDisplayName": "film.FilmDatabase / films_findManyFilm"
          },
          "parameters": [
            {
              "type": {
                "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                "parameters": [],
                "name": "VyneQlQuery",
                "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                "namespace": "vyne.vyneQl",
                "shortDisplayName": "VyneQlQuery",
                "longDisplayName": "vyne.vyneQl.VyneQlQuery"
              },
              "name": "body",
              "metadata": [],
              "constraints": [],
              "typeDoc": null,
              "nullable": false,
              "defaultValue": null,
              "allConstraints": {},
              "typeName": {
                "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                "parameters": [],
                "name": "VyneQlQuery",
                "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                "namespace": "vyne.vyneQl",
                "shortDisplayName": "VyneQlQuery",
                "longDisplayName": "vyne.vyneQl.VyneQlQuery"
              }
            }
          ],
          "returnType": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<film.Film>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Film[]",
            "longDisplayName": "film.Film[]"
          },
          "metadata": [],
          "grammar": "vyneQl",
          "capabilities": [
            "SUM",
            "COUNT",
            "AVG",
            "MIN",
            "MAX",
            {
              "supportedOperations": [
                "EQUAL",
                "NOT_EQUAL",
                "IN",
                "LIKE",
                "GREATER_THAN",
                "LESS_THAN",
                "GREATER_THAN_OR_EQUAL_TO",
                "LESS_THAN_OR_EQUAL_TO"
              ]
            }
          ],
          "typeDoc": null,
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Array",
              "parameters": [
                {
                  "fullyQualifiedName": "film.Film",
                  "parameters": [],
                  "name": "Film",
                  "parameterizedName": "film.Film",
                  "namespace": "film",
                  "shortDisplayName": "Film",
                  "longDisplayName": "film.Film"
                }
              ],
              "name": "Array",
              "parameterizedName": "lang.taxi.Array<film.Film>",
              "namespace": "lang.taxi",
              "shortDisplayName": "Film[]",
              "longDisplayName": "film.Film[]"
            },
            "constraints": []
          },
          "operationType": "READ_ONLY",
          "hasFilterCapability": true,
          "supportedFilterOperations": [
            "EQUAL",
            "NOT_EQUAL",
            "IN",
            "LIKE",
            "GREATER_THAN",
            "LESS_THAN",
            "GREATER_THAN_OR_EQUAL_TO",
            "LESS_THAN_OR_EQUAL_TO"
          ],
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "film.Film",
                "parameters": [],
                "name": "Film",
                "parameterizedName": "film.Film",
                "namespace": "film",
                "shortDisplayName": "Film",
                "longDisplayName": "film.Film"
              }
            ],
            "name": "Array",
            "parameterizedName": "lang.taxi.Array<film.Film>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Film[]",
            "longDisplayName": "film.Film[]"
          },
          "schemaMemberKind": "OPERATION",
          "operationKind": "Query",
          "name": "films_findManyFilm",
          "memberQualifiedName": {
            "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
            "parameters": [],
            "name": "FilmDatabase@@films_findManyFilm",
            "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
            "namespace": "film",
            "shortDisplayName": "films_findManyFilm",
            "longDisplayName": "film.FilmDatabase / films_findManyFilm"
          },
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "film.FilmDatabase@@films_findManyFilm",
              "parameters": [],
              "name": "FilmDatabase@@films_findManyFilm",
              "parameterizedName": "film.FilmDatabase@@films_findManyFilm",
              "namespace": "film",
              "shortDisplayName": "films_findManyFilm",
              "longDisplayName": "film.FilmDatabase / films_findManyFilm"
            },
            "kind": "OPERATION"
          }
        }
      ],
      "fullyQualifiedName": "film.FilmDatabase",
      "memberQualifiedName": {
        "fullyQualifiedName": "film.FilmDatabase",
        "parameters": [],
        "name": "FilmDatabase",
        "parameterizedName": "film.FilmDatabase",
        "namespace": "film",
        "shortDisplayName": "FilmDatabase",
        "longDisplayName": "film.FilmDatabase"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "film.FilmDatabase",
          "parameters": [],
          "name": "FilmDatabase",
          "parameterizedName": "film.FilmDatabase",
          "namespace": "film",
          "shortDisplayName": "FilmDatabase",
          "longDisplayName": "film.FilmDatabase"
        },
        "kind": "SERVICE"
      }
    },
    {
      "name": {
        "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService",
        "parameters": [],
        "name": "KafkaService",
        "parameterizedName": "com.orbitalhq.films.announcements.KafkaService",
        "namespace": "com.orbitalhq.films.announcements",
        "shortDisplayName": "KafkaService",
        "longDisplayName": "com.orbitalhq.films.announcements.KafkaService"
      },
      "operations": [],
      "queryOperations": [],
      "streamOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "parameters": [],
            "name": "KafkaService@@newReleases",
            "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "namespace": "com.orbitalhq.films.announcements",
            "shortDisplayName": "newReleases",
            "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
          },
          "returnType": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [
              {
                "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "parameters": [],
                "name": "NewFilmReleaseAnnouncement",
                "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "namespace": "demo.netflix",
                "shortDisplayName": "NewFilmReleaseAnnouncement",
                "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
              }
            ],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
            "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
          },
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
                "parameters": [],
                "name": "KafkaOperation",
                "parameterizedName": "com.orbitalhq.kafka.KafkaOperation",
                "namespace": "com.orbitalhq.kafka",
                "shortDisplayName": "KafkaOperation",
                "longDisplayName": "com.orbitalhq.kafka.KafkaOperation"
              },
              "params": {
                "topic": "releases",
                "offset": "latest"
              }
            }
          ],
          "typeDoc": null,
          "parameters": [],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Stream",
              "parameters": [
                {
                  "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                  "parameters": [],
                  "name": "NewFilmReleaseAnnouncement",
                  "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                  "namespace": "demo.netflix",
                  "shortDisplayName": "NewFilmReleaseAnnouncement",
                  "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
                }
              ],
              "name": "Stream",
              "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
              "namespace": "lang.taxi",
              "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
              "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
            },
            "constraints": []
          },
          "operationType": "READ_ONLY",
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [
              {
                "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "parameters": [],
                "name": "NewFilmReleaseAnnouncement",
                "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "namespace": "demo.netflix",
                "shortDisplayName": "NewFilmReleaseAnnouncement",
                "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
              }
            ],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
            "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
          },
          "schemaMemberKind": "OPERATION",
          "operationKind": "Stream",
          "name": "newReleases",
          "memberQualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "parameters": [],
            "name": "KafkaService@@newReleases",
            "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "namespace": "com.orbitalhq.films.announcements",
            "shortDisplayName": "newReleases",
            "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
          },
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
              "parameters": [],
              "name": "KafkaService@@newReleases",
              "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
              "namespace": "com.orbitalhq.films.announcements",
              "shortDisplayName": "newReleases",
              "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
            },
            "kind": "OPERATION"
          }
        }
      ],
      "version": [
        {
          version: "0.0.1",
          link: "link...",
          type: "SemVer"
        },
        {
          version: "0.0.2",
          link: "link2...",
          type: "SemVer"
        }
      ],
      "tableOperations": [],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "com.orbitalhq.kafka.KafkaService",
            "parameters": [],
            "name": "KafkaService",
            "parameterizedName": "com.orbitalhq.kafka.KafkaService",
            "namespace": "com.orbitalhq.kafka",
            "shortDisplayName": "KafkaService",
            "longDisplayName": "com.orbitalhq.kafka.KafkaService"
          },
          "params": {
            "connectionName": "kafka"
          }
        }
      ],
      "sourceCode": [
        {
          "name": "[demo.vyne/films-demo/0.1.0]/io\\vyne\\films\\announcements\\KafkaService.taxi",
          "version": "0.0.0",
          "content": "@com.orbitalhq.kafka.KafkaService(connectionName = \"kafka\")\r\n      service KafkaService {\r\n         @com.orbitalhq.kafka.KafkaOperation(topic = \"releases\" , offset = \"latest\")\r\n         stream newReleases : Stream<NewFilmReleaseAnnouncement>\r\n      }",
          "packageIdentifier": {
            "organisation": "demo.vyne",
            "name": "films-demo",
            "version": "0.1.0",
            "unversionedId": "demo.vyne/films-demo",
            "id": "demo.vyne/films-demo/0.1.0",
            "uriSafeId": "demo.vyne:films-demo:0.1.0"
          },
          "language": "taxi",
          "path": null,
          "packageQualifiedName": "[demo.vyne/films-demo/0.1.0]/io\\vyne\\films\\announcements\\KafkaService.taxi",
          "id": "[demo.vyne/films-demo/0.1.0]/io\\vyne\\films\\announcements\\KafkaService.taxi:0.0.0",
          "contentHash": "0b0c3c",
          "fullHash": "86ae36f743de2e2bc83b45985bf2049d41b28276a761bf911aea6b1877e0b528"
        }
      ],
      "typeDoc": null,
      "lineage": null,
      "serviceKind": "Kafka",
      "schemaMemberKind": "SERVICE",
      "remoteOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "parameters": [],
            "name": "KafkaService@@newReleases",
            "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "namespace": "com.orbitalhq.films.announcements",
            "shortDisplayName": "newReleases",
            "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
          },
          "returnType": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [
              {
                "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "parameters": [],
                "name": "NewFilmReleaseAnnouncement",
                "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "namespace": "demo.netflix",
                "shortDisplayName": "NewFilmReleaseAnnouncement",
                "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
              }
            ],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
            "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
          },
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "com.orbitalhq.kafka.KafkaOperation",
                "parameters": [],
                "name": "KafkaOperation",
                "parameterizedName": "com.orbitalhq.kafka.KafkaOperation",
                "namespace": "com.orbitalhq.kafka",
                "shortDisplayName": "KafkaOperation",
                "longDisplayName": "com.orbitalhq.kafka.KafkaOperation"
              },
              "params": {
                "topic": "releases",
                "offset": "latest"
              }
            }
          ],
          "typeDoc": null,
          "parameters": [],
          "contract": {
            "returnType": {
              "fullyQualifiedName": "lang.taxi.Stream",
              "parameters": [
                {
                  "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                  "parameters": [],
                  "name": "NewFilmReleaseAnnouncement",
                  "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                  "namespace": "demo.netflix",
                  "shortDisplayName": "NewFilmReleaseAnnouncement",
                  "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
                }
              ],
              "name": "Stream",
              "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
              "namespace": "lang.taxi",
              "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
              "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
            },
            "constraints": []
          },
          "operationType": "READ_ONLY",
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Stream",
            "parameters": [
              {
                "fullyQualifiedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "parameters": [],
                "name": "NewFilmReleaseAnnouncement",
                "parameterizedName": "demo.netflix.NewFilmReleaseAnnouncement",
                "namespace": "demo.netflix",
                "shortDisplayName": "NewFilmReleaseAnnouncement",
                "longDisplayName": "demo.netflix.NewFilmReleaseAnnouncement"
              }
            ],
            "name": "Stream",
            "parameterizedName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement>",
            "namespace": "lang.taxi",
            "shortDisplayName": "Stream<NewFilmReleaseAnnouncement>",
            "longDisplayName": "lang.taxi.Stream<demo.netflix.NewFilmReleaseAnnouncement><demo.netflix.NewFilmReleaseAnnouncement>"
          },
          "schemaMemberKind": "OPERATION",
          "operationKind": "Stream",
          "name": "newReleases",
          "memberQualifiedName": {
            "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "parameters": [],
            "name": "KafkaService@@newReleases",
            "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
            "namespace": "com.orbitalhq.films.announcements",
            "shortDisplayName": "newReleases",
            "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
          },
          "schemaMemberReference": {
            "qualifiedName": {
              "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
              "parameters": [],
              "name": "KafkaService@@newReleases",
              "parameterizedName": "com.orbitalhq.films.announcements.KafkaService@@newReleases",
              "namespace": "com.orbitalhq.films.announcements",
              "shortDisplayName": "newReleases",
              "longDisplayName": "com.orbitalhq.films.announcements.KafkaService / newReleases"
            },
            "kind": "OPERATION"
          }
        }
      ],
      "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService",
      "memberQualifiedName": {
        "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService",
        "parameters": [],
        "name": "KafkaService",
        "parameterizedName": "com.orbitalhq.films.announcements.KafkaService",
        "namespace": "com.orbitalhq.films.announcements",
        "shortDisplayName": "KafkaService",
        "longDisplayName": "com.orbitalhq.films.announcements.KafkaService"
      },
      "schemaMemberReference": {
        "qualifiedName": {
          "fullyQualifiedName": "com.orbitalhq.films.announcements.KafkaService",
          "parameters": [],
          "name": "KafkaService",
          "parameterizedName": "com.orbitalhq.films.announcements.KafkaService",
          "namespace": "com.orbitalhq.films.announcements",
          "shortDisplayName": "KafkaService",
          "longDisplayName": "com.orbitalhq.films.announcements.KafkaService"
        },
        "kind": "SERVICE"
      }
    }
  ],
  "queries": []
} as any as Schema
