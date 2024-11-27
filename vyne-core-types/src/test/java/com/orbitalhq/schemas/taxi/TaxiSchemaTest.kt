package com.orbitalhq.schemas.taxi

import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.from
import com.orbitalhq.query.VyneQlGrammar
import io.kotest.matchers.booleans.shouldBeTrue
import lang.taxi.errors
import org.junit.Test

class TaxiSchemaTest {


   @Test
   fun `when schemas with dependencies are loaded in the wrong order the result compiles eventually`() {
      val packageA = SourcePackage(
         PackageMetadata.from("com.foo", "test-1", "0.1.0"),
         listOf(
            VersionedSource.sourceOnly(
               """service PersonService {
               | operation lookupPerson(PersonId): Person
               |}
            """.trimMargin()
            )
         )
      )
      val packageB = SourcePackage(
         PackageMetadata.from("com.foo", "test-1", "0.1.0"),
         listOf(
            VersionedSource.sourceOnly(
               """model Person {
                        | personId : PersonId inherits String
                        |}
            """.trimMargin()
            )
         )
      )
      val schema = TaxiSchema.from(listOf(packageB, packageA))
//      val schema = TaxiSchema.from(listOf(packageA, packageB))
      val operation = schema.service("PersonService").operation("lookupPerson")
      operation.parameters.shouldHaveSize(1)

   }

   @Test
   fun `calling from() with compiler errors returns empty schema`() {
      // This behaviour is important, otherwise the query server can fail to start
      val schema = TaxiSchema.from("i am invalid")
      schema.should.not.be.`null`
   }

   @Test
   fun `parses a schema with a message stream correctly`() {
      val service = TaxiSchema.from(
         """
         model Person
         service MyKafkaService {
            stream personEvents : Stream<Person>
         }
      """.trimIndent()
      ).service("MyKafkaService")
      service.streamOperations.should.have.size(1)
      val stream = service.streamOperations.single()
      stream.returnType.name.parameterizedName.should.equal("lang.taxi.Stream<Person>")
   }

   @Test
   fun `parses a schema with a table correctly`() {
      val service = TaxiSchema.from(
         """
         ${VyneQlGrammar.QUERY_TYPE_TAXI}

         namespace myTest {
            model Person
            service MyKafkaService {
               table person : Person[]
            }
         }
      """.trimIndent()
      ).service("myTest.MyKafkaService")
      service.tableOperations.should.have.size(1)
      val table = service.tableOperations.single()
      table.returnType.name.parameterizedName.should.equal("lang.taxi.Array<myTest.Person>")
   }


   @Test
   fun `formatted types are defined on fields`() {
      val schema = TaxiSchema.from(
         """
            @Format("dd/MM/yy'T'HH:mm:ss" )
            type MyDate inherits Instant

            model Person {
               fromType : MyDate
               @Format("yyyy-MM-dd HH:mm:ss")
               fromTypeWithFormat : MyDate

               @Format(offset = 60)
               fromTypeWithOffset : MyDate
            }
      """.trimIndent()
      )
      schema.type("MyDate").format!!.shouldContainExactly("dd/MM/yy'T'HH:mm:ss")
      val person = schema.type("Person")
      person.attribute("fromType").format!!.patterns.shouldContainExactly("dd/MM/yy'T'HH:mm:ss")
      person.attribute("fromTypeWithFormat").format!!.patterns.shouldContainExactly("yyyy-MM-dd HH:mm:ss")
      person.attribute("fromTypeWithOffset").format!!.patterns.shouldContainExactly("dd/MM/yy'T'HH:mm:ss")
      person.attribute("fromTypeWithOffset").format!!.utcZoneOffsetInMinutes!!.shouldBe(60)
   }

   // Should replace ${'STOCK_PRICE_TOPIC'}
   @Test
   fun `env variables defined in annotations are replaced when using single-quoted string template`() {
      val schema = TaxiSchema.from(
         """
         annotation KafkaOperation {
            topic : String
         }

         model StockPrice {
            ticker: Ticker inherits String
         }
         service QuotesService {
            @KafkaOperation(topic = "${"$"}{STOCK_PRICE_TOPIC}")
            stream quotes : Stream<StockPrice>
         }
      """.trimIndent(),
         environmentVariables = mapOf("STOCK_PRICE_TOPIC" to "prod.stockQuotes")
      )
      val metadata = schema.service("QuotesService")
         .streamOperations.first { it.name == "quotes" }
         .firstMetadata("KafkaOperation")
      metadata.params["topic"].shouldBe("prod.stockQuotes")
   }


   // Should replace ${"STOCK_PRICE_TOPIC"}
   @Test
   fun `env variables defined in annotations are replaced when using double-quoted string template`() {
      val schema = TaxiSchema.from(
         """
         annotation KafkaOperation {
            topic : String
         }

         model StockPrice {
            ticker: Ticker inherits String
         }
         service QuotesService {
            @KafkaOperation(topic = '${"$"}{STOCK_PRICE_TOPIC}')
            stream quotes : Stream<StockPrice>
         }
      """.trimIndent(),
         environmentVariables = mapOf("STOCK_PRICE_TOPIC" to "prod.stockQuotes")
      )
      val metadata = schema.service("QuotesService")
         .streamOperations.first { it.name == "quotes" }
         .firstMetadata("KafkaOperation")
      metadata.params["topic"].shouldBe("prod.stockQuotes")
   }

   // Should replace ${"STOCK_PRICE_TOPIC"}
   @Test
   fun `env variables defined in annotations that are not present in env variables are reported as an error`() {
      val schema = TaxiSchema.from(
         """
         annotation KafkaOperation {
            topic : String
         }

         model StockPrice {
            ticker: Ticker inherits String
         }
         service QuotesService {
            @KafkaOperation(topic = '${"$"}{STOCK_PRICE_TOPIC}')
            stream quotes : Stream<StockPrice>
         }
      """.trimIndent()
      )
      schema.compilerMessages.errors().shouldHaveSize(1)
      schema.compilerMessages.errors().single()
         .detailMessage.shouldBe("Annotation KafkaOperation specifies env variable STOCK_PRICE_TOPIC which is not defined")
   }

   @Test
   fun `sum types of queries are present as anonymous types`() {
      val schema = TaxiSchema.from(
         """
namespace foo.test

model Tweet {
   id : TweetId inherits Int
   user : UserId
}
model User {
   id : UserId inherits Int
}
      """.trimIndent()
      )
      val (taxiQuery, b, querySchema) = schema.parseQuery("stream { Tweet | User }")
      val discoveryType = querySchema.type(taxiQuery.discoveryType!!.type)
      discoveryType.anonymousTypes.shouldHaveSize(1)
      val anonymousType = discoveryType.anonymousTypes.single()
      anonymousType.taxiType
   }

   @Test
   fun `array types have correct inner name`() {
      val schema = TaxiSchema.from(
         """
model Person {
   id : PersonId inherits Int
   friends : Person[]
}
      """.trimIndent()
      )
      val type = schema.type(schema.type("Person")
         .attribute("friends")
         .type)
      type
   }

}
