package com.orbitalhq

import com.orbitalhq.http.emptyResponse
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.json.parseJson
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class ProjectionsWithNullsTest {

   @Test
   fun `should not fail if a nullable property is not present on discovered data`(): Unit = runBlocking {
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
      stub.addResponse(
         "getJobStatus",
         vyne.parseJson("JobStatus", """{ "id" : 123, "status" : "Success" , "error" : null }""")
      )

      vyne.query(
         """
         find { JobList } as {
            jobs : JobId[] as (JobStatus) -> {
               state: Status
            }[]
         }
      """.trimIndent()
      )
         .firstRawObject()
         .shouldBe(mapOf("jobs" to listOf(mapOf("state" to "Success"))))
   }

   @Test
   fun `can coalesce and project a service that returns null`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                operation getCompany(BorrowerId):CompanyMemberData
            }
         """.trimIndent()
      )
      stub.addResponse("getCompany", TypedNull.create(vyne.type("CompanyMemberData")))

      val result = vyne.query(
         """given { accountId: BorrowerId = "24601" }
find { CompanyMemberData ?:  {} } as {
    id : ContactId
    fullName: ContactName
}
         """.trimMargin()
      )
         .typedInstances()
      result.shouldHaveSize(1)
      result.map { it.toRawObject() }
         .shouldBe(listOf(mapOf("id" to null, "fullName" to null)))
   }

   @Test
   fun `projecting a service that returns null array results in null`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                operation getCompany(BorrowerId):CompanyMemberData[]
            }
         """.trimIndent()
      )
      stub.addResponse("getCompany", TypedNull.create(vyne.type("CompanyMemberData[]")))

      val result = vyne.query(
         """given { accountId: BorrowerId = "24601" }
find { CompanyMemberData[] } as {
    id : ContactId
    fullName: ContactName
}[]
         """.trimMargin()
      )
         .typedInstances()
      result.shouldHaveSize(1)
      result.single().shouldBeInstanceOf<TypedNull>()
   }

   // ORB-986
   @Test
   fun `projecting a coalesced service that returns a value array results in the value array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                operation getCompany(BorrowerId):CompanyMemberData[]
            }
         """.trimIndent()
      )
      stub.addResponse(
         "getCompany", """
         [ { "contactId" : "123", "fullName" : "Jimmy" },
         { "contactId" : "456", "fullName" : "Jack" }
         ]
      """.trimIndent()
      )

      val result = vyne.query(
         """given { accountId: BorrowerId = "24601" }
// This is the test - CompanyMemberData is returned from a service, so the coalesce should not be used
find { CompanyMemberData[] ?: [{}] } as {
    id : ContactId
    fullName: ContactName
}[]
         """.trimMargin()
      )
         .typedInstances()
      result.shouldHaveSize(2)
      result.map { it.toRawObject() }
         .shouldBe(
            listOf(
               mapOf("id" to "123", "fullName" to "Jimmy"),
               mapOf("id" to "456", "fullName" to "Jack"),
            )
         )
   }

   // ORB-986
   @Test
   fun `projecting a coalesced service that returns a value results in the value`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                operation getCompany(BorrowerId):CompanyMemberData
            }
         """.trimIndent()
      )
      stub.addResponse(
         "getCompany", """
         { "contactId" : "123", "fullName" : "Jimmy" }
      """.trimIndent()
      )

      val result = vyne.query(
         """given { accountId: BorrowerId = "24601" }
// This is the test - CompanyMemberData is returned from a service, so the coalesce should not be used
find { CompanyMemberData ?: {} } as {
    id : ContactId
    fullName: ContactName
}
         """.trimMargin()
      )
         .rawObjects()
      result.shouldHaveSize(1)
      result
         .shouldBe(
            listOf(
               mapOf("id" to "123", "fullName" to "Jimmy")
            )
         )
   }

   @Test // ORB-980
   fun `should coalesce a null array`(): Unit = runBlocking {
      val (vyne, _) = testVyne(
         """
   type Surname inherits String
   type GivenName inherits String
   model Person {
     givenNames: GivenName[]
     surname: Surname
   }
""".trimIndent()
      )
      val result = vyne.query(
         """
         given { p: Person = {
             givenNames: null,
             surname : null
         }}
         find { Person } as {
             givens : GivenName[] ?: ['Jimmy']
             surname : Surname ?: 'Schmitt'
         }
      """.trimIndent()
      )
         .firstRawObject()
         .shouldBe(
            mapOf(
               "givens" to listOf("Jimmy"),
               "surname" to "Schmitt"
            )
         )
   }
}
