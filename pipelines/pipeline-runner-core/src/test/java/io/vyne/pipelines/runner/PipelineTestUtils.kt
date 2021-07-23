package io.vyne.pipelines.runner

import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne


object PipelineTestUtils {
   val personLoggedOnEvent = """{
         | "userId" : "jimmy"
         | }""".trimMargin()


   object PersonneLoggedInSchema {
      val source = """

         type PersonLoggedOnEvent {
            userId : UserId as String by jsonPath("/userId")
         }
         type alias Username as String

         service UserService {
            operation getUserNameFromId(UserId):Username
         }

         type UserEvent {
            id : UserId
            name : Username
         }

      """.trimIndent()

      val schema = TaxiSchema.from(source, "PersonLoggedOnEvent", "0.1.0")

   }

   /**
    * Stub Vyne with a stub service
    */
   fun pipelineTestVyne(): Pair<Vyne, StubService> {
      val src = PersonneLoggedInSchema.source.trimIndent()

      return testVyne(src)
   }


}

