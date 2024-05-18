package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.Benchmark
import com.orbitalhq.utils.asA
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import net.datafaker.Faker
import net.datafaker.transformations.Field
import net.datafaker.transformations.Field.compositeField
import net.datafaker.transformations.Field.field
import net.datafaker.transformations.JsonTransformer
import net.datafaker.transformations.Schema
import org.junit.Test
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.random.Random
import net.datafaker.transformations.Schema as FakerSchema

class PathTraversingFactSearcherPerformanceTest {
   val faker = Faker()
   val schema = TaxiSchema.from(

      """
      // These are types that aren't in the Film -> Actor graph
      // to test fast failure
      type TaxNumber inherits Int
      model StreamingService {
         name : StreamingServiceName
      }

      model Film {
         title : FilmTitle inherits String
        imdbScore : ImdbScore inherits Decimal
        yearReleased : YearReleased inherits String
        cast : Actor[]
        crew : Person[]
      }


      model Person {
         id : PersonId inherits Int
         firstName : FirstName inherits String
         lastName : LastName inherits String
         contactDetails : {
            emailAddress : EmailAddress inherits String
            streetAddress : {
               houseNumber : HouseNumber inherits String
               street : StreetName inherits String
               postcode : Postcode inherits String
               city : City inherits String
            }
         }
      }
      model Actor inherits Person {
         agentName : AgentName inherits String
      }


      model Catalog {
         films : Film[]
      }
   """.trimIndent()
   )

   @Test
   fun runPerformanceTest() {
      val films  = createFakeFilm(10)
      val typedInstance = TypedInstance.from(schema.type("Film[]"), films, schema)

      val expectedResult = CopyOnWriteFactBag(typedInstance, schema)
         .getFact(schema.type("FirstName"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY) as TypedCollection

      expectedResult.shouldHaveSize(200)

      Benchmark.benchmark("finding a collection of deeply nested fields", warmup = 100, iterations = 10000, timeUnit = TimeUnit.MICROSECONDS) {
         GlobalSchemaFactSearchCache.clear()
         val factBag = CopyOnWriteFactBag(typedInstance, schema)
         val result = factBag.getFactFast(schema.type("FirstName"), FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY)
         result.shouldBeInstanceOf<TypedCollection>()
            .shouldHaveSize(expectedResult.size)
      }
   }

   private fun createFakeFilm(rowCount: Int): List<Map<String,Any>> {
      return (0 until rowCount).map {
         mapOf(
            "title" to faker.book().title(),
            "yearReleased" to Random.nextInt(1950, 2024),
            "imdbScore" to (Random.nextInt(0, 50) / 10).toBigDecimal(),
            "cast" to (0 until 10).map { createFakePerson() },
            "crew" to (0 until 10).map { createFakePerson() }
         )
      }
   }

   private fun createFakePerson(): Map<String, Any> {
      val name = faker.name()
      val address = faker.address()

      return mapOf(
         "id" to Random.nextInt(),
         // Since this is a field we test with, make sure each value is unique
         "firstName" to name.firstName() + "_" + Random.nextInt().toString(),
         "lastName" to name.lastName(),
         "agentName" to faker.name().fullName(),
         "contactDetails" to mapOf(
            "emailAddress" to faker.internet().emailAddress(),
            "streetAddress" to mapOf(
               "houseNumber" to address.buildingNumber(),
               "street" to address.streetName(),
               "postcode" to address.postcode(),
               "city" to address.city()
            )
         )

      )
   }

}
