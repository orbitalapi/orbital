package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.firstTypedObject
import io.vyne.models.json.JsonModelParser
import io.vyne.models.json.parseJson
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
//import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.fail

class TypedObjectTest {

   val traderJson = """
       {
         "username" : "EUR_Trader",
         "jurisdiction" : "EUR",
         "limit" : {
            "currency" : "USD",
            "value" : 100
         }
       }
   """.trimIndent()

   lateinit var schema: TaxiSchema

   @Before
   fun setup() {
      val taxiDef = """
     type Money {
        currency : String
        value : Decimal
    }
    type Trader {
        username : String
        jurisdiction : String
        limit : Money
    }"""

      schema = TaxiSchema.from(taxiDef)
   }


   @Test
   fun canUnwrapTypedObject() {
      val trader = TypedInstance.from(schema.type("Trader"), traderJson, schema)
      val raw = trader.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun canParseJsonUsingTypedInstanceFrom() {
      val trader = TypedInstance.from(schema.type("Trader"), traderJson, schema, source = Provided)
      val raw = trader.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);

   }

   @Test
   fun canConvertTypedInstanceToTypeNamedObject() {
      val trader = TypedInstance.from(schema.type("Trader"), traderJson, schema, source = Provided)
      val raw = trader.toTypeNamedInstance()
      val stringType = "lang.taxi.String".fqn()
      val decimalType = "lang.taxi.Decimal".fqn()
      val expected = TypeNamedInstance(
         typeName = "Trader".fqn(), value = mapOf(
            "username" to TypeNamedInstance(stringType, "EUR_Trader", Provided),
            "jurisdiction" to TypeNamedInstance(stringType, "EUR", Provided),
            "limit" to TypeNamedInstance(
               "Money".fqn(), mapOf(
                  "currency" to TypeNamedInstance(stringType, "USD", Provided),
                  "value" to TypeNamedInstance(decimalType, 100.toBigDecimal(), Provided)
               ), Provided
            )
         ), source = Provided
      )
      expect(raw).to.equal(expected)
   }

   @Test
   fun canConvertTypedCollectionToTypeNamedObject() {
      val trader = TypedInstance.from(schema.type("Trader"), traderJson, schema, source = Provided)
      val collection = TypedCollection.arrayOf(schema.type("Trader"), listOf(trader))
      val raw = collection.toTypeNamedInstance() as List<TypeNamedInstance>
      expect(raw).to.have.size(1)
      expect(raw.first().typeName).to.equal("Trader")
   }

   @Test
   fun canParseFromNestedMapToTypedObject() {
      val traderAttributes = jacksonObjectMapper().readValue<Map<String, Any>>(traderJson)
      val instance = TypedObject.fromAttributes(schema.type("Trader"), traderAttributes, schema, source = Provided)
      val raw = instance.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun `can use default values in json when parsing`() {
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : FirstName as String
            title : Title as String by default("foo")
         }
      """.trimIndent()
      )
      val json = """{ "firstName" : "Jimmy" }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("foo")
   }

   @Test // This specific test because of a bug found where this was failing
   fun `can use default value of empty string in json when parsing`() {
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : FirstName as String
            title : Title as String by default("")
         }
      """.trimIndent()
      )
      val json = """{ "firstName" : "Jimmy" }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("")
   }

   @Test
   fun `can ingest boolean value into enum with synonym`() {
      val schema = TaxiSchema.from(
         """
         enum LivingOrDead {
            Alive, Dead
         }
         enum IsAlive {
            `true` synonym of LivingOrDead.Alive,
            `false` synonym of LivingOrDead.Dead
         }
         model Person {
            name : Name as String
            isAlive : IsAlive
         }
         model OutputPerson {
            name : Name
            livingOrDead : LivingOrDead
         }
      """.trimIndent()
      )
      val (vyne, _) = testVyne(schema)
      val json = """{ "name" : "Bernstein", "isAlive" : false }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      runBlocking {
         val buildResult = vyne.query().addFact(instance).build("OutputPerson")
         val output = buildResult.firstTypedObject()
         output["livingOrDead"].value!!.should.equal("Dead")
      }
   }

   @Test
   fun `can ingest boolean value into enum with synonym when read using an accessor`() {
      val schema = TaxiSchema.from(
         """
         enum LivingOrDead {
            Alive, Dead
         }
         enum IsAlive {
            `true` synonym of LivingOrDead.Alive,
            `false` synonym of LivingOrDead.Dead
         }
         model Person {
            name : Name as String
            isAlive : IsAlive by jsonPath("$.living")
         }
         model OutputPerson {
            name : Name
            livingOrDead : LivingOrDead
         }
      """.trimIndent()
      )
      val (vyne, _) = testVyne(schema)
      val json = """{ "name" : "Bernstein", "living" : false }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      runBlocking {
         val buildResult = vyne.query().addFact(instance).build("OutputPerson")
         val output = buildResult.firstTypedObject()
         output["livingOrDead"].value!!.should.equal("Dead")
      }
   }


   @Test
   fun `calling getAll with a path that includes a collection returns all matching elements from within collection`() {
      val (vyne, _) = testVyne(
         """
         model Film {
            title : String
            cast : Actor[]
         }
         model Actor {
            name : String
         }
      """.trimIndent()
      )
      val instance = vyne.parseJson(
         "Film", """{
         |  "title" : "Star Wars",
         |  "cast" : [
         |     {"name" : "Mark" },
         |     {"name" : "Carrie" }
         |  ]
         |}
      """.trimMargin()
      ) as TypedObject
      val result = instance.getAllAtPath("cast.name")
         .map { it.value!!.toString() }
      result.should.contain.elements("Mark", "Carrie")

      val collections = instance.getAllAtPath("cast").map { it.toRawObject() }
      collections.should.equal(
         listOf(
            listOf(
               mapOf("name" to "Mark"),
               mapOf("name" to "Carrie"),
            )
         )
      )
   }

}
