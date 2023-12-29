package com.orbitalhq.query.chat

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asSourcePackage
import com.orbitalhq.query.VyneQlGrammar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.packages.TaxiSourcesLoader
import java.nio.file.Paths

class ChatQueryParserTest : DescribeSpec({
   val apiKey = ""

   // Don't run in CI/CD, just exploring.
   xdescribe("exploring the ChatGPT query API") {
      it("should use chatGPT to parse a query") {
         val schema = TaxiSchema.from(
            """
         [[ The name of the movie ]]
         type Title inherits String

         [[ The Id of the film ]]
         type FilmId inherits Int

         [[ The review score ]]
         type Rating inherits Int

         [[ The text of a review ]]
         type ReviewText inherits String

         [[ The duration of a movie ]]
         type DurationInMinutes inherits Int

         [[ A review of a film ]]
         model FilmReview

         [[ The number of unqiue viewers who have watched a film ]]
         model ViewCount

         [[ The total number of watched minutes ]]
         model MinutesWatched

         model FilmAnalyticsEvent {
            uniqueViewers: ViewCount
            minutesWatched : MinutesWatched
         }

         service StreamService {
            operation reviews:Stream<FilmReview>
            operation analytics:Stream<FilmAnalytics>
         }
      """.trimIndent()
         )
         val parser = ChatQueryParser(apiKey)
         val taxiQl = parser.parseToTaxiQl(schema, "Tell me how long 'Gladiator' is, and it's review score")
         TODO()

      }

      it("exploring prompt engineering") {
         val parser = ChatQueryParser(apiKey)
         val taxiQL = SourcePackage(
            PackageMetadata.from("com.orbitalhq", "core-types", "1.0.0"),
            listOf(
               VersionedSource(
                  "TaxiQL",
                  version = "0.1.0",
                  VyneQlGrammar.QUERY_TYPE_TAXI
               )
            )
         )
         val path = Paths.get("/home/martypitt/dev/orbital-demos/hz-demo/taxi")
         val srcPackage = TaxiSourcesLoader.loadPackage(path).asSourcePackage()

         val schema = TaxiSchema.from(listOf(taxiQL, srcPackage))
         parser.parseToTaxiQl(schema, "Build a real time stream of trades. Include the name of the trader, the name of the instrument, the quantity and hit price on the order. Also include the last traded price for the same instrument, and the ESG score (calculated as the average of the Environmental, Social and Governance pillar scores) for the instrument")
      }
   }
})
