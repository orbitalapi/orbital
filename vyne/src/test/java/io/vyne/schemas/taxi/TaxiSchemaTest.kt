package io.vyne.schemas.taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Modifier
import io.vyne.schemas.TypeFullView
import junit.framework.Assert.fail
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class TaxiSchemaTest {

   @Test
   fun when_creatingMultipleSchemas_then_importsAreRespected() {
      val srcA = """
namespace foo

type alias Age as Int
type Person {
   name : FirstName as String
}""".trimIndent()

      val srcB: String = """
import foo.Person

namespace bar

type alias PageNumber as Int
type Book {
   author : foo.Person
}
      """.trimIndent()

      val srcC: String = """
import bar.Book

namespace baz

type alias PhoneNumber as String
 type Library {
   inventory : bar.Book[]
}
      """.trimIndent()
      // This intentionally has no imports, to ensure it's still picked up correctl
      val srcD = """
namespace bak

type Video {}
      """.trimIndent()

      // Jumble the order of imported sources
      val schema = TaxiSchema.from(listOf(srcC, srcA, srcB, srcD).map { VersionedSource.sourceOnly(it) })
      schema.type("baz.Library").attribute("inventory").type.parameterizedName.should.equal("lang.taxi.Array<bar.Book>")

      val missingTypes = listOf("baz.PhoneNumber", "baz.Library", "bar.PageNumber", "bar.Book", "foo.Age", "foo.Person", "bak.Video").mapNotNull {
         if (!schema.hasType(it)) {
            it
         } else null
      }
      if (missingTypes.isNotEmpty()) {
         fail("The following types are missing: ${missingTypes.joinToString(",")}")
      }
   }

   @Test(expected = CircularDependencyInSourcesException::class)
   fun when_importingMultipeSources_that_circularDependenciesAreNotPermitted() {
      val srcA = """
import baz.Library
namespace foo

type Person {
   name : FirstName as String
    // this creates a circular dependency
   nearestLibrary: baz.Library
}""".trimIndent()

      val srcB: String = """
import foo.Person

namespace bar

type Book {
   author : foo.Person
}
      """.trimIndent()

      val srcC: String = """
import bar.Book

namespace baz

 type Library {
   inventory : bar.Book[]
}"""
      val schemas = TaxiSchema.from(listOf(srcC, srcB, srcA).map { VersionedSource.sourceOnly(it) })
   }

   @Test
   fun given_taxiTypeIsClosed_when_imported_then_vyneTypeShouldBeClosed() {

      val src = """
 closed type Money {
   currency : Currency as String
   value : MoneyValue as Int
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      expect(schema.type("Money").modifiers).to.contain(Modifier.CLOSED)
   }

   @Test
   fun given_taxiFieldIsClosed_when_imported_then_vyneFieldShouldBeClosed() {
      val src = """
 type Money {
   closed currency : Currency as String
   value : MoneyValue as Int
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      expect(schema.type("Money").attribute("currency").modifiers).to.contain(FieldModifier.CLOSED)

   }

   @Test
   fun when_importingTypeExtensionsAcrossMutlipleFiles_then_theyAreApplied() {

      val srcA = """
namespace foo

type Customer {}""".trimIndent()

      val srcB = """
import foo.Customer

namespace bar

[[ I am docs ]]
type extension Customer {}
      """.trimIndent()
      val schema = TaxiSchema.from(listOf(srcB, srcA).map { VersionedSource.sourceOnly(it) })
      schema.type("foo.Customer").typeDoc.should.equal("I am docs")

   }

   @Test
   fun canAliasACollection() {
      val src = """
type Person {
   firstName : FirstName as String
}

type alias PersonCollection as Person[]
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val type = schema.type("PersonCollection")
      type.isTypeAlias.should.be.`true`
      type.aliasForTypeName!!.parameterizedName.should.equal("lang.taxi.Array<Person>")
      type.isCollection.should.be.`true`
   }

   @Test
   fun fieldsWithUnderscoresInNamesAreConvertedIntoVyneTypesCorrectly() {
      val src = """
type Sample {
   something_S0 : Something_S0 as String
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val type = schema.type("Sample")
      val field = type.attribute("something_S0")
      field.type.fullyQualifiedName.should.equal("Something_S0")
   }

   @Test
   fun parsesArrayTypes() {
      val src = """
         type alias Email as String
         type Person {
            emails : Email[]
         }
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      schema.type("Person").attribute("emails").type.parameterizedName.should.equal("lang.taxi.Array<Email>")
   }

   @Test
   fun typeAliasTypesAreSetCorrectly() {
      val src = """
         type alias Name as String
         type FirstName inherits Name
         type alias GivenName as FirstName
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      schema.type("Name").aliasForType!!.name.fullyQualifiedName.should.equal("lang.taxi.String")
      schema.type("GivenName").aliasForType!!.name.fullyQualifiedName.should.equal("FirstName")
   }

   @Test
   fun inheritenceIsPopulatedCorrectly() {
      val src = """
         type alias Name as String
         type FirstName inherits Name
         type alias GivenName as FirstName
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val type = schema.type("FirstName")
      type.inherits.should.have.size(1)
      type.inherits.map { it.name.fullyQualifiedName }.should.contain("Name")
      schema.type("FirstName").inherits.should.have.size(1)
      schema.type("FirstName").inheritanceGraph.should.have.size(1)
      schema.type("GivenName").inheritanceGraph.map { it.name.fullyQualifiedName }.should.contain("Name")
   }

   @Test
   fun canSerializeTypeToJson() {
      val src = """
   type Customer {
      email : CustomerEmailAddress as String
      id : CustomerId as String
      name : CustomerName as String
      postcode : Postcode as String
   }
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      val type = schema.type("Customer")
      val json = jacksonObjectMapper()
         .writerWithDefaultPrettyPrinter()
         .withView(TypeFullView::class.java)
         .writeValueAsString(type)

      JSONAssert.assertEquals(expectedJson,json,true)
   }

   private val expectedJson = """
{
  "name" : {
    "fullyQualifiedName" : "Customer",
    "parameters" : [ ],
    "name" : "Customer",
    "parameterizedName" : "Customer",
    "namespace" : "",
    "longDisplayName" : "Customer",
    "shortDisplayName" : "Customer"
  },
  "attributes" : {
    "email" : {
      "type" : {
        "fullyQualifiedName" : "CustomerEmailAddress",
        "parameters" : [ ],
        "name" : "CustomerEmailAddress",
        "parameterizedName" : "CustomerEmailAddress",
        "namespace" : "",
        "longDisplayName" : "CustomerEmailAddress",
        "shortDisplayName" : "CustomerEmailAddress"
      },
      "modifiers" : [ ],
      "accessor" : null,
      "readCondition" : null,
      "typeDoc" : null,
      "constraints" : [ ]
    },
    "id" : {
      "type" : {
        "fullyQualifiedName" : "CustomerId",
        "parameters" : [ ],
        "name" : "CustomerId",
        "parameterizedName" : "CustomerId",
        "namespace" : "",
        "longDisplayName" : "CustomerId",
        "shortDisplayName" : "CustomerId"
      },
      "modifiers" : [ ],
      "accessor" : null,
      "readCondition" : null,
      "typeDoc" : null,
      "constraints" : [ ]
    },
    "name" : {
      "type" : {
        "fullyQualifiedName" : "CustomerName",
        "parameters" : [ ],
        "name" : "CustomerName",
        "parameterizedName" : "CustomerName",
        "namespace" : "",
        "longDisplayName" : "CustomerName",
        "shortDisplayName" : "CustomerName"
      },
      "modifiers" : [ ],
      "accessor" : null,
      "readCondition" : null,
      "typeDoc" : null,
      "constraints" : [ ]
    },
    "postcode" : {
      "type" : {
        "fullyQualifiedName" : "Postcode",
        "parameters" : [ ],
        "name" : "Postcode",
        "parameterizedName" : "Postcode",
        "namespace" : "",
        "longDisplayName" : "Postcode",
        "shortDisplayName" : "Postcode"
      },
      "modifiers" : [ ],
      "accessor" : null,
      "readCondition" : null,
      "typeDoc" : null,
      "constraints" : [ ]
    }
  },
  "modifiers" : [ ],
  "metadata" : [ ],
  "aliasForType" : null,
  "inheritsFrom" : [ ],
  "enumValues" : [ ],
  "sources" : [ {
    "name" : "<unknown>",
    "version" : "0.0.0",
    "content" : "type Customer {\n   email : CustomerEmailAddress as String\n   id : CustomerId as String\n   name : CustomerName as String\n   postcode : Postcode as String\n}",
    "id" : "<unknown>:0.0.0"
  } ],
  "typeParameters" : [ ],
  "typeDoc" : "",
  "isTypeAlias" : false,
  "isParameterType" : false,
  "isClosed" : false,
  "isPrimitive" : false,
  "fullyQualifiedName" : "Customer",
  "memberQualifiedName" : {
    "fullyQualifiedName" : "Customer",
    "parameters" : [ ],
    "name" : "Customer",
    "parameterizedName" : "Customer",
    "namespace" : "",
    "longDisplayName" : "Customer",
    "shortDisplayName" : "Customer"
  },
  "isCollection" : false,
  "underlyingTypeParameters" : [ ],
  "collectionType" : null,
  "isScalar" : false
}
   """.trimIndent()
}


