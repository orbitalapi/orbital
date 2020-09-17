package io.vyne.schemas.taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Modifier
import io.vyne.schemas.TypeFullView
import io.vyne.schemas.fqn
import io.vyne.utils.log
import junit.framework.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

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

   fun when_importingMultipeSources_that_circularDependenciesArePermitted() {
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
   fun annotationsOnTypesArePopulated() {
      val metadata = TaxiSchema.from("""
         @Foo
         model Person {
            name : String
         }
      """.trimIndent()).type("Person").metadata
      metadata.should.have.size(1)
      metadata.first().name.fullyQualifiedName.should.equal("Foo")
      metadata.first().params.should.be.empty
   }
   @Test
   fun annotationsWithAttributesAreMapped() {
      val metadata = TaxiSchema.from("""
         @Foo(name = 'baz', count = 1)
         model Person {
            name : String
         }
      """.trimIndent()).type("Person").metadata
      metadata.should.have.size(1)
      val annotation = metadata.first()
      annotation.params["name"].should.equal("baz")
      annotation.params["count"].should.equal(1)
   }

   @Test
   fun annotationsWithParamsOnFieldsAreMapped() {
      val metadata = TaxiSchema.from("""
         model Person {
            @Foo(name = 'baz', count = 1)
            name : String
         }
      """.trimIndent()).type("Person").attribute("name").metadata
      metadata.should.have.size(1)
      val annotation = metadata.first()
      annotation.params["name"].should.equal("baz")
      annotation.params["count"].should.equal(1)
   }

   @Test
   fun annotationsOnFieldsAreMapped() {
      val metadata = TaxiSchema.from("""
         model Person {
            @Foo
            name : String
         }
      """.trimIndent()).type("Person").attribute("name").metadata
      metadata.should.have.size(1)
      val annotation = metadata.first()
      annotation.name.fullyQualifiedName.should.equal("Foo")
      annotation.params.should.be.empty
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

      JSONAssert.assertEquals(expectedJson, json, true)
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
      "typeDoc" : null,
      "defaultValue" : null,
      "nullable" : false,
      "typeDisplayName" : "CustomerEmailAddress",
      "metadata" : [ ],
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
      "typeDoc" : null,
      "defaultValue" : null,
      "nullable" : false,
      "typeDisplayName" : "CustomerId",
      "metadata" : [ ],
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
      "typeDoc" : null,
      "defaultValue" : null,
      "nullable" : false,
      "typeDisplayName" : "CustomerName",
      "metadata" : [ ],
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
      "typeDoc" : null,
      "defaultValue" : null,
      "nullable" : false,
      "typeDisplayName" : "Postcode",
      "metadata" : [ ],
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
    "id" : "<unknown>:0.0.0",
    "contentHash" : "7063d8"
  } ],
  "typeParameters" : [ ],
  "typeDoc" : "",
  "isTypeAlias" : false,
  "format" : null,
  "hasFormat" : false,
  "isCalculated" : false,
  "basePrimitiveTypeName" : null,
  "isParameterType" : false,
  "isClosed" : false,
  "isPrimitive" : false,
  "fullyQualifiedName" : "Customer",
  "unformattedTypeName" : null,
  "longDisplayName" : "Customer",
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


   @Test
   @Ignore("This test is handy for debugging issues with type hashes")
   fun loadSchemaFromDirectory() {
      val taxonomyPath = Paths.get("C:\\dev\\workspace\\lens\\test-schemas\\taxonomy\\src")
      val schemaContentList: List<VersionedSource> = Files.walk(taxonomyPath)
         .filter { it.toFile().isFile }
         .filter { it.toFile().extension.equals("taxi") }
         .map {
            val content = String(Files.readAllBytes(it.toAbsolutePath()))
            VersionedSource(it.toAbsolutePath().toString(), "1.0.0", content)
         }.collect(Collectors.toList())
      val schema = TaxiSchema.from(schemaContentList)

      val versionedType = schema.versionedType("<type-name-to-lookup>".fqn())
      log().info("Hash: ${versionedType.versionHash}")
      log().info("VersionedName: ${versionedType.versionedName}")
   }
}


