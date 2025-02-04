package com.orbitalhq

import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.utils.asA
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class ConstraintsInProjectionsTest {

   @Test
   fun `loads from service if requested data is array`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
type ApplicationId inherits String
type BorrowerId inherits String

closed model LoanApplication {
  applicationId : ApplicationId
  borrower : BorrowerId
}

service LoanApplicationService {
 operation getByLoanId(loanApplicationId: ApplicationId): LoanApplication(ApplicationId ==loanApplicationId)
 operation getPreviousLoanApplications(borrowerId: BorrowerId): LoanApplication[](BorrowerId == borrowerId)
}
      """.trimIndent()
      )
      stub.addResponse(
         "getByLoanId",
         """{ "applicationId" : "123" , "borrower"  :  "Jimmy" }""",
         modifyDataSource = true
      )
      stub.addResponse(
         "getPreviousLoanApplications",
         """[{ "applicationId" : "234" , "borrower" : "Jimmy" }]""",
         modifyDataSource = true
      )

      val result = vyne.query(
         """
         find {
             LoanApplication(ApplicationId == '123')
         } as (application:LoanApplication) -> {
             applicationId,
             borrower,

             // This is the test.
             // The value returned here should come from a different
             // service call than the one returning the value we're currently
              // projecting, as they have different, incompatible constraints
             previousApplications: LoanApplication[](BorrowerId == application.borrower)
         }
      """.trimIndent()
      )
         .firstTypedObject()
      val previousApplicationsCall = stub.calls["getPreviousLoanApplications"]
      previousApplicationsCall.shouldHaveSize(1)
      previousApplicationsCall.single().map { it.toRawObject() }.single().shouldBe("Jimmy")

      // Verify the previousApplications were sourced from the
      // correct API call
      result["previousApplications"].source
         .asA<OperationResultReference>()
         .operationDisplayName.shouldBe("getPreviousLoanApplications")
      result.toRawObject()
         .shouldBe(
            mapOf(
               "applicationId" to "123",
               "borrower" to "Jimmy",
               "previousApplications" to listOf(mapOf("applicationId" to "234", "borrower" to "Jimmy"))
            )
         )
   }


   @Test
   fun `loads from service if requested data is scalar`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
type ApplicationId inherits String
type BorrowerId inherits String

closed model LoanApplication {
  applicationId : ApplicationId
  borrower : BorrowerId
}

service LoanApplicationService {
 operation getByLoanId(loanApplicationId: ApplicationId): LoanApplication(ApplicationId ==loanApplicationId)
 operation getPreviousLoanApplications(borrowerId: BorrowerId): LoanApplication(BorrowerId == borrowerId)
}
      """.trimIndent()
      )
      stub.addResponse(
         "getByLoanId",
         """{ "applicationId" : "123" , "borrower"  :  "Jimmy" }""",
         modifyDataSource = true
      )
      stub.addResponse(
         "getPreviousLoanApplications",
         """{ "applicationId" : "234" , "borrower" : "Jimmy" }""",
         modifyDataSource = true
      )

      val result = vyne.query(
         """
         find {
             LoanApplication(ApplicationId == '123')
         } as (application:LoanApplication) -> {
             applicationId,
             borrower,

             // This is the test.
             // The value returned here should come from a different
             // service call than the one returning the value we're currently
              // projecting, as they have different, incompatible constraints
             previousApplications: LoanApplication(BorrowerId == application.borrower)
         }
      """.trimIndent()
      )
         .firstTypedObject()
      val previousApplicationsCall = stub.calls["getPreviousLoanApplications"]
      previousApplicationsCall.shouldHaveSize(1)
      previousApplicationsCall.single().map { it.toRawObject() }.single().shouldBe("Jimmy")

      // Verify the previousApplications were sourced from the
      // correct API call
      result["previousApplications"].source
         .asA<OperationResultReference>()
         .operationDisplayName.shouldBe("getPreviousLoanApplications")
      result.toRawObject()
         .shouldBe(
            mapOf(
               "applicationId" to "123",
               "borrower" to "Jimmy",
               "previousApplications" to mapOf("applicationId" to "234", "borrower" to "Jimmy")
            )
         )
   }

   @Test
   fun `can use a nested attribute in a projection constraint`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
type ApplicationId inherits String
type BorrowerId inherits String

closed model LoanApplication {
  applicationId : ApplicationId
  borrower : {
    id :  BorrowerId
  }
}

service LoanApplicationService {
 operation getByLoanId(loanApplicationId: ApplicationId): LoanApplication(ApplicationId ==loanApplicationId)
 operation getPreviousLoanApplications(borrowerId: BorrowerId): LoanApplication(BorrowerId == borrowerId)
}
      """.trimIndent()
      )
      stub.addResponse(
         "getByLoanId",
         """{ "applicationId" : "123" , "borrower"  :  { "id" : "Jimmy"  } }""",
         modifyDataSource = true
      )
      stub.addResponse(
         "getPreviousLoanApplications",
         """{ "applicationId" : "234" , "borrower" : { "id" : "Jimmy" } }""",
         modifyDataSource = true
      )

      val result = vyne.query(
         """
         find {
             LoanApplication(ApplicationId == '123')
         } as (application:LoanApplication) -> {
             applicationId,
             borrower,

             // This is the test.
             // The value returned here should come from a different
             // service call than the one returning the value we're currently
              // projecting, as they have different, incompatible constraints
             previousApplications: LoanApplication(BorrowerId == application.borrower.id)
         }
      """.trimIndent()
      )
         .firstTypedObject()
      val previousApplicationsCall = stub.calls["getPreviousLoanApplications"]
      previousApplicationsCall.shouldHaveSize(1)
      previousApplicationsCall.single().map { it.toRawObject() }.single().shouldBe("Jimmy")

      // Verify the previousApplications were sourced from the
      // correct API call
      result["previousApplications"].source
         .asA<OperationResultReference>()
         .operationDisplayName.shouldBe("getPreviousLoanApplications")
      result.toRawObject()
         .shouldBe(
            mapOf(
               "applicationId" to "123",
               "borrower" to mapOf("id" to "Jimmy"),
               "previousApplications" to mapOf("applicationId" to "234", "borrower" to mapOf("id" to "Jimmy"))
            )
         )
   }

   @Test
   fun `loads from service if requested data is scalar and contract operators are inverted`(): Unit = runBlocking {
      val (vyne, stub) = testVyne(
         """
type ApplicationId inherits String
type BorrowerId inherits String

closed model LoanApplication {
  applicationId : ApplicationId
  borrower : BorrowerId
}

service LoanApplicationService {
 operation getByLoanId(loanApplicationId: ApplicationId): LoanApplication(ApplicationId ==loanApplicationId)

 // This is key to the test.
 // The operation contract is defined in the opposite order than the contract
 // we'll be searching for in the query
 operation getPreviousLoanApplications(borrowerId: BorrowerId): LoanApplication(BorrowerId == borrowerId)
}
      """.trimIndent()
      )
      stub.addResponse(
         "getByLoanId",
         """{ "applicationId" : "123" , "borrower"  :  "Jimmy" }""",
         modifyDataSource = true
      )
      stub.addResponse(
         "getPreviousLoanApplications",
         """{ "applicationId" : "234" , "borrower" : "Jimmy" }""",
         modifyDataSource = true
      )

      val result = vyne.query(
         """
         find {
             LoanApplication(ApplicationId == '123')
         } as (application:LoanApplication) -> {
             applicationId,
             borrower,
             //
             // This is the test!
             // The contract is declared as BorrowerId == borrowerId
             // It shouldn't matter that they are inverted
             //
             previousApplications: LoanApplication(application.borrower == BorrowerId)
         }
      """.trimIndent()
      )
         .firstTypedObject()
      val previousApplicationsCall = stub.calls["getPreviousLoanApplications"]
      previousApplicationsCall.shouldHaveSize(1)
      previousApplicationsCall.single().map { it.toRawObject() }.single().shouldBe("Jimmy")

      // Verify the previousApplications were sourced from the
      // correct API call
      result["previousApplications"].source
         .asA<OperationResultReference>()
         .operationDisplayName.shouldBe("getPreviousLoanApplications")
      result.toRawObject()
         .shouldBe(
            mapOf(
               "applicationId" to "123",
               "borrower" to "Jimmy",
               "previousApplications" to mapOf("applicationId" to "234", "borrower" to "Jimmy")
            )
         )
   }

   @Test
   fun `can project a discovered nested attribute to an array`():Unit = kotlinx.coroutines.runBlocking {
      val (vyne, stub) = testVyne(
         """
         closed model Deal {
           id : DealId inherits Int
           borrowerId : BorrowerId inherits Int
         }
         closed model ExistingDeals {
           deals: Deal[]
         }

         service DealApi {
           operation getDeal(DealId):Deal(...)
           operation getExistingDeals(BorrowerId):ExistingDeals(...)
         }
      """
      )
      stub.addResponse("getDeal", """{ "id" : 1, "borrowerId" : 100 }""")
      stub.addResponse(
         "getExistingDeals", """{
 "deals" : [
    { "id" : 2, "borrowerId" : 100 } ,
    { "id" : 3, "borrowerId" : 100 }
  ]
}"""
      )
      val result = vyne.query(
         """
given { id: DealId = 1}
find { Deal(DealId == id )} as(deal: Deal) -> {
//    existing :  ExistingDeals(BorrowerId == deal.borrowerId)
    existing : Deal[] = ExistingDeals(BorrowerId == deal.borrowerId) as Deal[]
    ...
}
"""
      ).firstRawObject()
      result.shouldBe(
         mapOf(
            "existing" to listOf(
               mapOf("id" to 2, "borrowerId" to 100),
               mapOf("id" to 3, "borrowerId" to 100),
            ),
            "id" to 1,
            "borrowerId" to 100
         )
      )

   }

   @Test
   fun `can use a nested type reference on a nested projection with constraint`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
type AccountId inherits String
type AccountName inherits String
type AccountCompanyNumber inherits String

type BorrowerId inherits AccountId
type BorrowerName inherits AccountName
type BorrowerCompanyNumber inherits AccountCompanyNumber

type DealId inherits String
type DealName inherits String


model Borrower {
  id: BorrowerId,
  name: BorrowerName,
  companyNumber: BorrowerCompanyNumber
}

closed model BorrowerResponse {
  data: Borrower[]
}

model Deal {
  id: DealId
  name: DealName
  borrower: {
    id: BorrowerId
    name: BorrowerName
  }
}

closed model DealResponse {
  data: Deal[]
}

service DealService {
  operation getDeal(dealId: DealId): DealResponse(...)
  operation getDealByBorrower(borrowerId: BorrowerId): DealResponse(...)
}""".trimIndent())

      stub.addResponse("getDeal", """{
    "data":[{
        "id": "123",
        "name": "Example Deal",
        "borrower": {
            "id": "546",
            "name": "Borrower Account"
        }
    }]
}""")
      stub.addResponse("getDealByBorrower", """{
    "data": [
        {
            "id": "4565",
            "name": "The First Deal",
            "borrower": {
                "id": "546",
                "name": "Borrower Account"
            }
        },
        {
            "id": "2344",
            "name": "The Second Deal",
            "borrower": {
                "id": "546",
                "name": "Borrower Account"
            }
        },
        {
            "id": "8956765",
            "name": "The Final Deal",
            "borrower": {
                "id": "546",
                "name": "Borrower Account"
            }
        },
        {
            "id": "123",
            "name": "Example Deal",
            "borrower": {
                "id": "546",
                "name": "Borrower Account"
            }
        }
    ]
}""")
      val result = vyne.query("""
given {dealId: DealId = "123"}
find {DealResponse(DealId == dealId)} as  (deal:Deal = first(Deal[])) -> {
    id: DealId,
    name: DealName,
    existingDeals: DealResponse(BorrowerId == deal::BorrowerId)
  }
      """.trimIndent())
         .firstRawObject()
      result["existingDeals"].shouldBeInstanceOf<Map<String,Any>>()
         .get("data")
         .shouldBeInstanceOf<List<*>>()
         .shouldHaveSize(4)

      // Should've called the right endpoint
      stub.calls["getDealByBorrower"].shouldHaveSize(1)
   }

}
