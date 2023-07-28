package io.vyne.pipelines.jet.transform

import io.kotest.matchers.shouldBe
import io.vyne.models.json.parseJson
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

@Testcontainers
@RunWith(SpringRunner::class)
class PipelineWithTransformationQueryTest : BaseJetIntegrationTest() {
   @Test
   fun `when a transformation query is defined then it is invoked`() {
      val taxiDef = """
         model Input {
            filmId : FilmId inherits String
         }

         model Film {
            title : FilmTitle inherits String
            review : ReviewScore inherits Int
         }

         service FilmService {
            operation lookupFilm(FilmId): Film
         }

         type PosterQuote inherits String
         model Output {
            score : ReviewScore
            posterQuote : PosterQuote
         }
      """.trimIndent()
      val testSetup = jetWithSpringAndVyne(taxiDef)
      val schema = testSetup.schema
      testSetup.stubService.addResponse(
         "lookupFilm",
         parseJson(schema, "Film", """{ "title" : "Star Wars" , "review" : 4.99 }""")
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(
         testSetup.applicationContext,
         targetType = "Output"
      )
      val pipelineSpec = PipelineSpec(
         name = "transforming",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "filmId" : "star-1"  }"""),
            typeName = "Input".fqn()
         ),
         transformation = """find { Film } as {
            |score : ReviewScore
            |posterQuote : PosterQuote = "Triffic."
            |}""".trimMargin(),
         outputs = listOf(outputSpec)
      )
      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
         listSinkTarget.size > 0
      }
      val message = listSinkTarget.list.single() as TypedInstanceContentProvider
      message.content.toRawObject().shouldBe(
         mapOf(
            "score" to 4,
            "posterQuote" to "Triffic."
         )
      )
   }
}
