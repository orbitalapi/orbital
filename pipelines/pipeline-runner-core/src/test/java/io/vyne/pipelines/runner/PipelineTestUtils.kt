package io.vyne.pipelines.runner

import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.testVyne

object PipelineTestUtils {
   val personLoggedOnEvent = """{
         | "userId" : "jimmy"
         | }""".trimMargin()
   fun pipelineTestVyne(): Pair<Vyne, StubService> {
      val src = """
type PersonLoggedOnEvent {
   userId : UserId as String
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
      return testVyne(src)
   }
}
