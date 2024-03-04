package com.orbitalhq

import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.junit.Test

class ProjectionsWithNullsTest {

   @Test
   fun `should not fail if a nullable property is not present on discovered data`():Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            model JobError {
               message : ErrorMessage inherits String
            }
            model JobStatus {
               id : JobId inherits Int
               status : Status inherits String
               error : JobError?
            }
            model JobList {
               ids : JobId[]
            }
            service JobService {
               operation getJobs():JobList
               operation getJobStatus(JobId):JobStatus
            }
         """.trimIndent()
      )
      stub.addResponse("getJobs", vyne.parseJson("JobList", """{ "ids" : [123] } """))
      stub.addResponse("getJobStatus", vyne.parseJson("JobStatus", """{ "id" : 123, "status" : "Success" , "error" : null }"""))

      vyne.query("""
         find { JobList } as {
            jobs : JobId[] as (JobStatus) -> {
               state: Status
            }[]
         }
      """.trimIndent())
         .firstRawObject()
         .shouldBe(mapOf("jobs" to listOf(mapOf("state" to "Success"))))
   }
}
