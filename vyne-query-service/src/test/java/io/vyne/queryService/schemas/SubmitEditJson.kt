package io.vyne.queryService.schemas

object SubmitEditJson {
   const val JSON = """{
  "types": [
    {
      "name": {
        "fullyQualifiedName": "customer.CustomerId",
        "parameters": [],
        "parameterizedName": "customer.CustomerId",
        "name": "CustomerId",
        "namespace": "customer",
        "shortDisplayName": "CustomerId",
        "longDisplayName": "customer.CustomerId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "parameterizedName": "lang.taxi.Int",
          "name": "Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/CustomerId.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type CustomerId inherits Int\n}",
          "id": "customer/CustomerId.taxi:0.0.0",
          "contentHash": "1edd44"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "parameterizedName": "lang.taxi.Int",
        "name": "Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.CustomerId",
      "longDisplayName": "customer.CustomerId",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.CustomerId",
        "parameters": [],
        "parameterizedName": "customer.CustomerId",
        "name": "CustomerId",
        "namespace": "customer",
        "shortDisplayName": "CustomerId",
        "longDisplayName": "customer.CustomerId"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.FirstName",
        "parameters": [],
        "parameterizedName": "customer.FirstName",
        "name": "FirstName",
        "namespace": "customer",
        "shortDisplayName": "FirstName",
        "longDisplayName": "customer.FirstName"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "parameterizedName": "lang.taxi.String",
          "name": "String",
          "namespace": "lang.taxi",
          "shortDisplayName": "String",
          "longDisplayName": "lang.taxi.String"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/FirstName.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type FirstName inherits String\n}",
          "id": "customer/FirstName.taxi:0.0.0",
          "contentHash": "a4ced9"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.String",
        "parameters": [],
        "parameterizedName": "lang.taxi.String",
        "name": "String",
        "namespace": "lang.taxi",
        "shortDisplayName": "String",
        "longDisplayName": "lang.taxi.String"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.FirstName",
      "longDisplayName": "customer.FirstName",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.FirstName",
        "parameters": [],
        "parameterizedName": "customer.FirstName",
        "name": "FirstName",
        "namespace": "customer",
        "shortDisplayName": "FirstName",
        "longDisplayName": "customer.FirstName"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.LastName",
        "parameters": [],
        "parameterizedName": "customer.LastName",
        "name": "LastName",
        "namespace": "customer",
        "shortDisplayName": "LastName",
        "longDisplayName": "customer.LastName"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "parameterizedName": "lang.taxi.String",
          "name": "String",
          "namespace": "lang.taxi",
          "shortDisplayName": "String",
          "longDisplayName": "lang.taxi.String"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/LastName.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type LastName inherits String\n}",
          "id": "customer/LastName.taxi:0.0.0",
          "contentHash": "891e4a"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.String",
        "parameters": [],
        "parameterizedName": "lang.taxi.String",
        "name": "String",
        "namespace": "lang.taxi",
        "shortDisplayName": "String",
        "longDisplayName": "lang.taxi.String"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.LastName",
      "longDisplayName": "customer.LastName",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.LastName",
        "parameters": [],
        "parameterizedName": "customer.LastName",
        "name": "LastName",
        "namespace": "customer",
        "shortDisplayName": "LastName",
        "longDisplayName": "customer.LastName"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.Email",
        "parameters": [],
        "parameterizedName": "customer.Email",
        "name": "Email",
        "namespace": "customer",
        "shortDisplayName": "Email",
        "longDisplayName": "customer.Email"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.String",
          "parameters": [],
          "parameterizedName": "lang.taxi.String",
          "name": "String",
          "namespace": "lang.taxi",
          "shortDisplayName": "String",
          "longDisplayName": "lang.taxi.String"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/Email.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type Email inherits String\n}",
          "id": "customer/Email.taxi:0.0.0",
          "contentHash": "96677f"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.String",
        "parameters": [],
        "parameterizedName": "lang.taxi.String",
        "name": "String",
        "namespace": "lang.taxi",
        "shortDisplayName": "String",
        "longDisplayName": "lang.taxi.String"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.Email",
      "longDisplayName": "customer.Email",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.Email",
        "parameters": [],
        "parameterizedName": "customer.Email",
        "name": "Email",
        "namespace": "customer",
        "shortDisplayName": "Email",
        "longDisplayName": "customer.Email"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.Activebool",
        "parameters": [],
        "parameterizedName": "customer.Activebool",
        "name": "Activebool",
        "namespace": "customer",
        "shortDisplayName": "Activebool",
        "longDisplayName": "customer.Activebool"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Boolean",
          "parameters": [],
          "parameterizedName": "lang.taxi.Boolean",
          "name": "Boolean",
          "namespace": "lang.taxi",
          "shortDisplayName": "Boolean",
          "longDisplayName": "lang.taxi.Boolean"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/Activebool.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type Activebool inherits Boolean\n}",
          "id": "customer/Activebool.taxi:0.0.0",
          "contentHash": "5c3e78"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Boolean",
        "parameters": [],
        "parameterizedName": "lang.taxi.Boolean",
        "name": "Boolean",
        "namespace": "lang.taxi",
        "shortDisplayName": "Boolean",
        "longDisplayName": "lang.taxi.Boolean"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.Activebool",
      "longDisplayName": "customer.Activebool",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.Activebool",
        "parameters": [],
        "parameterizedName": "customer.Activebool",
        "name": "Activebool",
        "namespace": "customer",
        "shortDisplayName": "Activebool",
        "longDisplayName": "customer.Activebool"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.CreateDate",
        "parameters": [],
        "parameterizedName": "customer.CreateDate",
        "name": "CreateDate",
        "namespace": "customer",
        "shortDisplayName": "CreateDate",
        "longDisplayName": "customer.CreateDate"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Date",
          "parameters": [],
          "parameterizedName": "lang.taxi.Date",
          "name": "Date",
          "namespace": "lang.taxi",
          "shortDisplayName": "Date",
          "longDisplayName": "lang.taxi.Date"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/CreateDate.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type CreateDate inherits Date\n}",
          "id": "customer/CreateDate.taxi:0.0.0",
          "contentHash": "f892f7"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": [
        "yyyy-MM-dd"
      ],
      "hasFormat": true,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Date",
        "parameters": [],
        "parameterizedName": "lang.taxi.Date",
        "name": "Date",
        "namespace": "lang.taxi",
        "shortDisplayName": "Date",
        "longDisplayName": "lang.taxi.Date"
      },
      "hasExpression": false,
      "unformattedTypeName": {
        "fullyQualifiedName": "customer.CreateDate",
        "parameters": [],
        "parameterizedName": "customer.CreateDate",
        "name": "CreateDate",
        "namespace": "customer",
        "shortDisplayName": "CreateDate",
        "longDisplayName": "customer.CreateDate"
      },
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.CreateDate",
      "longDisplayName": "customer.CreateDate(yyyy-MM-dd)",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.CreateDate",
        "parameters": [],
        "parameterizedName": "customer.CreateDate",
        "name": "CreateDate",
        "namespace": "customer",
        "shortDisplayName": "CreateDate",
        "longDisplayName": "customer.CreateDate"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.LastUpdate",
        "parameters": [],
        "parameterizedName": "customer.LastUpdate",
        "name": "LastUpdate",
        "namespace": "customer",
        "shortDisplayName": "LastUpdate",
        "longDisplayName": "customer.LastUpdate"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Instant",
          "parameters": [],
          "parameterizedName": "lang.taxi.Instant",
          "name": "Instant",
          "namespace": "lang.taxi",
          "shortDisplayName": "Instant",
          "longDisplayName": "lang.taxi.Instant"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/LastUpdate.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type LastUpdate inherits Instant\n}",
          "id": "customer/LastUpdate.taxi:0.0.0",
          "contentHash": "a4a9e8"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": [
        "yyyy-MM-dd'T'HH:mm:ss[.SSS]X"
      ],
      "hasFormat": true,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Instant",
        "parameters": [],
        "parameterizedName": "lang.taxi.Instant",
        "name": "Instant",
        "namespace": "lang.taxi",
        "shortDisplayName": "Instant",
        "longDisplayName": "lang.taxi.Instant"
      },
      "hasExpression": false,
      "unformattedTypeName": {
        "fullyQualifiedName": "customer.LastUpdate",
        "parameters": [],
        "parameterizedName": "customer.LastUpdate",
        "name": "LastUpdate",
        "namespace": "customer",
        "shortDisplayName": "LastUpdate",
        "longDisplayName": "customer.LastUpdate"
      },
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.LastUpdate",
      "longDisplayName": "customer.LastUpdate(yyyy-MM-dd'T'HH:mm:ss[.SSS]X)",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.LastUpdate",
        "parameters": [],
        "parameterizedName": "customer.LastUpdate",
        "name": "LastUpdate",
        "namespace": "customer",
        "shortDisplayName": "LastUpdate",
        "longDisplayName": "customer.LastUpdate"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.Active",
        "parameters": [],
        "parameterizedName": "customer.Active",
        "name": "Active",
        "namespace": "customer",
        "shortDisplayName": "Active",
        "longDisplayName": "customer.Active"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "parameterizedName": "lang.taxi.Int",
          "name": "Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/Active.taxi",
          "version": "0.0.0",
          "content": "namespace customer {\n   type Active inherits Int\n}",
          "id": "customer/Active.taxi:0.0.0",
          "contentHash": "9233c9"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "parameterizedName": "lang.taxi.Int",
        "name": "Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.Active",
      "longDisplayName": "customer.Active",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.Active",
        "parameters": [],
        "parameterizedName": "customer.Active",
        "name": "Active",
        "namespace": "customer",
        "shortDisplayName": "Active",
        "longDisplayName": "customer.Active"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "customer.Customer",
        "parameters": [],
        "parameterizedName": "customer.Customer",
        "name": "Customer",
        "namespace": "customer",
        "shortDisplayName": "Customer",
        "longDisplayName": "customer.Customer"
      },
      "attributes": {
        "customer_id": {
          "type": {
            "fullyQualifiedName": "customer.CustomerId",
            "parameters": [],
            "parameterizedName": "customer.CustomerId",
            "name": "CustomerId",
            "namespace": "customer",
            "shortDisplayName": "CustomerId",
            "longDisplayName": "customer.CustomerId"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "customer.CustomerId",
          "metadata": [
            {
              "name": {
                "fullyQualifiedName": "Id",
                "parameters": [],
                "parameterizedName": "Id",
                "name": "Id",
                "namespace": "",
                "shortDisplayName": "Id",
                "longDisplayName": "Id"
              },
              "params": {}
            }
          ],
          "sourcedBy": null
        },
        "store_id": {
          "type": {
            "fullyQualifiedName": "store.StoreId",
            "parameters": [],
            "parameterizedName": "store.StoreId",
            "name": "StoreId",
            "namespace": "store",
            "shortDisplayName": "StoreId",
            "longDisplayName": "store.StoreId"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "store.StoreId",
          "metadata": [],
          "sourcedBy": null
        },
        "first_name": {
          "type": {
            "fullyQualifiedName": "customer.FirstName",
            "parameters": [],
            "parameterizedName": "customer.FirstName",
            "name": "FirstName",
            "namespace": "customer",
            "shortDisplayName": "FirstName",
            "longDisplayName": "customer.FirstName"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "customer.FirstName",
          "metadata": [],
          "sourcedBy": null
        },
        "last_name": {
          "type": {
            "fullyQualifiedName": "customer.LastName",
            "parameters": [],
            "parameterizedName": "customer.LastName",
            "name": "LastName",
            "namespace": "customer",
            "shortDisplayName": "LastName",
            "longDisplayName": "customer.LastName"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "customer.LastName",
          "metadata": [],
          "sourcedBy": null
        },
        "email": {
          "type": {
            "fullyQualifiedName": "customer.Email",
            "parameters": [],
            "parameterizedName": "customer.Email",
            "name": "Email",
            "namespace": "customer",
            "shortDisplayName": "Email",
            "longDisplayName": "customer.Email"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "customer.Email",
          "metadata": [],
          "sourcedBy": null
        },
        "address_id": {
          "type": {
            "fullyQualifiedName": "address.AddressId",
            "parameters": [],
            "parameterizedName": "address.AddressId",
            "name": "AddressId",
            "namespace": "address",
            "shortDisplayName": "AddressId",
            "longDisplayName": "address.AddressId"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "address.AddressId",
          "metadata": [],
          "sourcedBy": null
        },
        "activebool": {
          "type": {
            "fullyQualifiedName": "customer.Activebool",
            "parameters": [],
            "parameterizedName": "customer.Activebool",
            "name": "Activebool",
            "namespace": "customer",
            "shortDisplayName": "Activebool",
            "longDisplayName": "customer.Activebool"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "customer.Activebool",
          "metadata": [],
          "sourcedBy": null
        },
        "create_date": {
          "type": {
            "fullyQualifiedName": "customer.CreateDate",
            "parameters": [],
            "parameterizedName": "customer.CreateDate",
            "name": "CreateDate",
            "namespace": "customer",
            "shortDisplayName": "CreateDate",
            "longDisplayName": "customer.CreateDate"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": false,
          "typeDisplayName": "customer.CreateDate",
          "metadata": [],
          "sourcedBy": null
        },
        "last_update": {
          "type": {
            "fullyQualifiedName": "customer.LastUpdate",
            "parameters": [],
            "parameterizedName": "customer.LastUpdate",
            "name": "LastUpdate",
            "namespace": "customer",
            "shortDisplayName": "LastUpdate",
            "longDisplayName": "customer.LastUpdate"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "customer.LastUpdate",
          "metadata": [],
          "sourcedBy": null
        },
        "active": {
          "type": {
            "fullyQualifiedName": "customer.Active",
            "parameters": [],
            "parameterizedName": "customer.Active",
            "name": "Active",
            "namespace": "customer",
            "shortDisplayName": "Active",
            "longDisplayName": "customer.Active"
          },
          "modifiers": [],
          "typeDoc": null,
          "defaultValue": null,
          "nullable": true,
          "typeDisplayName": "customer.Active",
          "metadata": [],
          "sourcedBy": null
        }
      },
      "modifiers": [],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.Table",
            "parameters": [],
            "parameterizedName": "io.vyne.jdbc.Table",
            "name": "Table",
            "namespace": "io.vyne.jdbc",
            "shortDisplayName": "Table",
            "longDisplayName": "io.vyne.jdbc.Table"
          },
          "params": {
            "table": "customer",
            "schema": "public",
            "connection": "asfdf"
          }
        }
      ],
      "aliasForType": null,
      "inheritsFrom": [],
      "enumValues": [],
      "sources": [
        {
          "name": "customer/Customer.taxi",
          "version": "0.0.0",
          "content": "import store.StoreId\nimport address.AddressId\nimport io.vyne.jdbc.Table\nnamespace customer {\n   @io.vyne.jdbc.Table(table = \"customer\" , schema = \"public\" , connection = \"asfdf\")\n         model Customer {\n            @Id customer_id : CustomerId\n            store_id : store.StoreId\n            first_name : FirstName\n            last_name : LastName\n            email : Email?\n            address_id : address.AddressId\n            activebool : Activebool\n            create_date : CreateDate\n            last_update : LastUpdate?\n            active : Active?\n         }\n}",
          "id": "customer/Customer.taxi:0.0.0",
          "contentHash": "df3c48"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": null,
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "customer.Customer",
      "longDisplayName": "customer.Customer",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.Customer",
        "parameters": [],
        "parameterizedName": "customer.Customer",
        "name": "Customer",
        "namespace": "customer",
        "shortDisplayName": "Customer",
        "longDisplayName": "customer.Customer"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": false
    },
    {
      "name": {
        "fullyQualifiedName": "store.StoreId",
        "parameters": [],
        "parameterizedName": "store.StoreId",
        "name": "StoreId",
        "namespace": "store",
        "shortDisplayName": "StoreId",
        "longDisplayName": "store.StoreId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "parameterizedName": "lang.taxi.Int",
          "name": "Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "store/StoreId.taxi",
          "version": "0.0.0",
          "content": "namespace store {\n   type StoreId inherits Int\n}",
          "id": "store/StoreId.taxi:0.0.0",
          "contentHash": "e1794a"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "parameterizedName": "lang.taxi.Int",
        "name": "Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "store.StoreId",
      "longDisplayName": "store.StoreId",
      "memberQualifiedName": {
        "fullyQualifiedName": "store.StoreId",
        "parameters": [],
        "parameterizedName": "store.StoreId",
        "name": "StoreId",
        "namespace": "store",
        "shortDisplayName": "StoreId",
        "longDisplayName": "store.StoreId"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    },
    {
      "name": {
        "fullyQualifiedName": "address.AddressId",
        "parameters": [],
        "parameterizedName": "address.AddressId",
        "name": "AddressId",
        "namespace": "address",
        "shortDisplayName": "AddressId",
        "longDisplayName": "address.AddressId"
      },
      "attributes": {},
      "modifiers": [],
      "metadata": [],
      "aliasForType": null,
      "inheritsFrom": [
        {
          "fullyQualifiedName": "lang.taxi.Int",
          "parameters": [],
          "parameterizedName": "lang.taxi.Int",
          "name": "Int",
          "namespace": "lang.taxi",
          "shortDisplayName": "Int",
          "longDisplayName": "lang.taxi.Int"
        }
      ],
      "enumValues": [],
      "sources": [
        {
          "name": "address/AddressId.taxi",
          "version": "0.0.0",
          "content": "namespace address {\n   type AddressId inherits Int\n}",
          "id": "address/AddressId.taxi:0.0.0",
          "contentHash": "0549cc"
        }
      ],
      "typeParameters": [],
      "typeDoc": "",
      "isTypeAlias": false,
      "offset": null,
      "format": null,
      "hasFormat": false,
      "basePrimitiveTypeName": {
        "fullyQualifiedName": "lang.taxi.Int",
        "parameters": [],
        "parameterizedName": "lang.taxi.Int",
        "name": "Int",
        "namespace": "lang.taxi",
        "shortDisplayName": "Int",
        "longDisplayName": "lang.taxi.Int"
      },
      "hasExpression": false,
      "unformattedTypeName": null,
      "isParameterType": false,
      "isClosed": false,
      "isPrimitive": false,
      "fullyQualifiedName": "address.AddressId",
      "longDisplayName": "address.AddressId",
      "memberQualifiedName": {
        "fullyQualifiedName": "address.AddressId",
        "parameters": [],
        "parameterizedName": "address.AddressId",
        "name": "AddressId",
        "namespace": "address",
        "shortDisplayName": "AddressId",
        "longDisplayName": "address.AddressId"
      },
      "underlyingTypeParameters": [],
      "isCollection": false,
      "isStream": false,
      "collectionType": null,
      "isScalar": true
    }
  ],
  "services": [
    {
      "name": {
        "fullyQualifiedName": "customer.CustomerService",
        "parameters": [],
        "parameterizedName": "customer.CustomerService",
        "name": "CustomerService",
        "namespace": "customer",
        "shortDisplayName": "CustomerService",
        "longDisplayName": "customer.CustomerService"
      },
      "operations": [],
      "queryOperations": [
        {
          "qualifiedName": {
            "fullyQualifiedName": "customer.CustomerService@@customerQuery",
            "parameters": [],
            "parameterizedName": "customer.CustomerService@@customerQuery",
            "name": "CustomerService@@customerQuery",
            "namespace": "customer",
            "shortDisplayName": "customerQuery",
            "longDisplayName": "customer.CustomerService / customerQuery"
          },
          "parameters": [
            {
              "typeName": {
                "fullyQualifiedName": "vyne.vyneQl.VyneQlQuery",
                "parameters": [],
                "parameterizedName": "vyne.vyneQl.VyneQlQuery",
                "name": "VyneQlQuery",
                "namespace": "vyne.vyneQl",
                "shortDisplayName": "VyneQlQuery",
                "longDisplayName": "vyne.vyneQl.VyneQlQuery"
              },
              "name": "querySpec",
              "metadata": [],
              "constraints": []
            }
          ],
          "returnTypeName": {
            "fullyQualifiedName": "lang.taxi.Array",
            "parameters": [
              {
                "fullyQualifiedName": "customer.Customer",
                "parameters": [],
                "parameterizedName": "customer.Customer",
                "name": "Customer",
                "namespace": "customer",
                "shortDisplayName": "Customer",
                "longDisplayName": "customer.Customer"
              }
            ],
            "parameterizedName": "lang.taxi.Array<customer.Customer>",
            "name": "Array",
            "namespace": "lang.taxi",
            "shortDisplayName": "Customer[]",
            "longDisplayName": "customer.Customer[]"
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
                  "fullyQualifiedName": "customer.Customer",
                  "parameters": [],
                  "parameterizedName": "customer.Customer",
                  "name": "Customer",
                  "namespace": "customer",
                  "shortDisplayName": "Customer",
                  "longDisplayName": "customer.Customer"
                }
              ],
              "parameterizedName": "lang.taxi.Array<customer.Customer>",
              "name": "Array",
              "namespace": "lang.taxi",
              "shortDisplayName": "Customer[]",
              "longDisplayName": "customer.Customer[]"
            },
            "constraints": []
          },
          "operationType": null,
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
          "name": "customerQuery",
          "memberQualifiedName": {
            "fullyQualifiedName": "customer.CustomerService@@customerQuery",
            "parameters": [],
            "parameterizedName": "customer.CustomerService@@customerQuery",
            "name": "CustomerService@@customerQuery",
            "namespace": "customer",
            "shortDisplayName": "customerQuery",
            "longDisplayName": "customer.CustomerService / customerQuery"
          }
        }
      ],
      "metadata": [
        {
          "name": {
            "fullyQualifiedName": "io.vyne.jdbc.DatabaseService",
            "parameters": [],
            "parameterizedName": "io.vyne.jdbc.DatabaseService",
            "name": "DatabaseService",
            "namespace": "io.vyne.jdbc",
            "shortDisplayName": "DatabaseService",
            "longDisplayName": "io.vyne.jdbc.DatabaseService"
          },
          "params": {
            "connection": "asfdf"
          }
        }
      ],
      "sourceCode": [
        {
          "name": "customer/CustomerService.taxi",
          "version": "0.0.0",
          "content": "import vyne.vyneQl.VyneQlQuery\nnamespace customer {\n   @io.vyne.jdbc.DatabaseService(connection = \"asfdf\")\n         service CustomerService {\n            vyneQl query customerQuery(querySpec: vyne.vyneQl.VyneQlQuery):lang.taxi.Array<customer.Customer> with capabilities {\n               sum,\n               count,\n               avg,\n               min,\n               max,\n               filter(==,!=,in,like,>,<,>=,<=)\n            }\n         }\n}",
          "id": "customer/CustomerService.taxi:0.0.0",
          "contentHash": "24de2b"
        }
      ],
      "typeDoc": null,
      "lineage": null,
      "qualifiedName": "customer.CustomerService",
      "memberQualifiedName": {
        "fullyQualifiedName": "customer.CustomerService",
        "parameters": [],
        "parameterizedName": "customer.CustomerService",
        "name": "CustomerService",
        "namespace": "customer",
        "shortDisplayName": "CustomerService",
        "longDisplayName": "customer.CustomerService"
      }
    }
  ]
}"""
}
