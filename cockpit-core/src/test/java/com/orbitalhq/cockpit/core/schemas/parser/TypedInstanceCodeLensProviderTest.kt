package com.orbitalhq.cockpit.core.schemas.parser

import com.orbitalhq.models.json.parseJson
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class TypedInstanceCodeLensProviderTest : DescribeSpec({

   describe("Generating code hints for TypedInstance") {
      val schema = TaxiSchema.from("""
         model Actor inherits Person {
               agent : Person
            }
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Movie {
            actors : Actor[]
            director : Person
            title : Title inherits String
         }
      """.trimIndent())

      it("should provide insights for typed instance") {
         val json = """{
            | "actors" : [
            |  { "firstName" : "Mel", "lastName" : "Gibson" , "agent" : { "firstName" : "Johnny", "lastName" : "Cashpott"} },
            |  { "firstName" : "Jack", "lastName" : "Spratt" , "agent" : { "firstName" : "Johnny", "lastName" : "Cashpott"}  }
            |],
            | "director" : { "firstName" : "Steve", "lastName" : "Speilburg" },
            |  "title" : "Star Wars"
            |}
         """.trimMargin()
         val typedInstance = parseJson(schema, "Movie", json)
         val (formattedJson, hints) = TypedInstanceInlayHintProvider().generateHints(
            typedInstance
         )
         val formattedJsonLines = formattedJson.lines()
         hints.shouldNotBeNull()
         hints.forEach { hint ->
            val line = formattedJsonLines[hint.start.line - 1]
            val char = line.get(hint.start.char + 1)
            char.shouldBe(':')
         }
      }
   }

})
