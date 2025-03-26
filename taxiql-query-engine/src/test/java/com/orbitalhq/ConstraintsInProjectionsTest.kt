package com.orbitalhq

import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.utils.asA
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
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

   @Test
   fun `a constraint type reference correctly resolves against the object when using a nested projection with constraint`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model DealResponse {
            loanApplicationId : LoanApplicationId inherits String
            deals : Deal[]
         }
         model Deal {
            dealId : DealId inherits String
            borrowerId : BorrowerId inherits String
         }
         model BorrowerAccount {
            id : BorrowerId
            borrowerName : BorrowerName inherits String
         }
         service DealApi {
            operation forLoanApplicationId(LoanApplicationId):DealResponse(...)
            operation forBorrowerId(BorrowerId):DealResponse(...)
            operation getBorrower(BorrowerId):BorrowerAccount(...)
         }
      """
         )
         stub.addResponse("getBorrower", """{ "id" : "b1", "borrowerName" : "Jimmy" }""")
         // The two api calls return different values.
         // The point is to ensure that the values from forBorrowerId are used in the
         // correct place (as defined by the constraints in the query below)
         stub.addResponse(
            "forLoanApplicationId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d1", "borrowerId" : "b1"  }
          ]}""".trimMargin()
         )
         stub.addResponse(
            "forBorrowerId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d2", "borrowerId" : "b1" }, { "dealId" : "d3", "borrowerId" : "b1"  }
          ]}""".trimMargin()
         )


         val result = vyne.query(
            """
         given { loanApplicationId: LoanApplicationId = "1234" }
         find { DealResponse(LoanApplicationId == loanApplicationId) } as (deal:Deal = first(Deal[])) -> {
             id: LoanApplicationId,
             borrower: BorrowerAccount as {
               name : BorrowerName,
               // This is the test.
               // We already have a Deal[] in scope on the parent DealResponse
               // But it's not the correct Deal[].
               // The test ensures that the result of this projection comes from
               // projecting the newly loaded DealResponse, not the existing one
               existingDeals: DealResponse(BorrowerId == deal::BorrowerId) as Deal[]
             }
         }
      """.trimIndent()
         )
            .firstRawObject()

         result.shouldBe(
            mapOf(
               "id" to "1234",
               "borrower" to mapOf(
                  "name" to "Jimmy",
                  "existingDeals" to listOf(
                     mapOf("dealId" to "d2", "borrowerId" to "b1"),
                     mapOf("dealId" to "d3", "borrowerId" to "b1"),
                  )

               )
            ),
         )

         val borrowerIdCallInputs = stub.calls["forBorrowerId"]
            .shouldHaveSize(1)
            .single()
         borrowerIdCallInputs.single().toRawObject().shouldBe("b1")

      }


   @Test
   fun `a constraint type reference correctly resolves against the object when using a nested projection with constraint and can then project with chained projection`(): Unit =
      runBlocking {
         val (vyne, stub) = testVyne(
            """
         model DealResponse {
            loanApplicationId : LoanApplicationId inherits String
            deals : Deal[]
         }
         model Deal {
            dealId : DealId inherits String
            borrowerId : BorrowerId inherits String
            amount : DealAmount inherits Int
         }
         model BorrowerAccount {
            id : BorrowerId
            borrowerName : BorrowerName inherits String
         }
         service DealApi {
            operation forLoanApplicationId(LoanApplicationId):DealResponse(...)
            operation forBorrowerId(BorrowerId):DealResponse(...)
            operation getBorrower(BorrowerId):BorrowerAccount(...)
         }
      """
         )
         stub.addResponse("getBorrower", """{ "id" : "b1", "borrowerName" : "Jimmy" }""")
         // The two api calls return different values.
         // The point is to ensure that the values from forBorrowerId are used in the
         // correct place (as defined by the constraints in the query below)
         stub.addResponse(
            "forLoanApplicationId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d1", "borrowerId" : "b1", "amount" : 100  }
          ]}""".trimMargin()
         )
         stub.addResponse(
            "forBorrowerId", """{
          "loanApplicationId" : "l1",
          "deals": [
          { "dealId" : "d2", "borrowerId" : "b1", "amount" : 200 }, { "dealId" : "d3", "borrowerId" : "b1", "amount" : 300  }
          ]}""".trimMargin()
         )


         val result = vyne.query(
            """
         given { loanApplicationId: LoanApplicationId = "1234" }
         find { DealResponse(LoanApplicationId == loanApplicationId) } as (deal:Deal = first(Deal[])) -> {
             id: LoanApplicationId,
             borrower: BorrowerAccount as {
               name : BorrowerName,
               // This is the test.
               // We already have a Deal[] in scope on the parent DealResponse
               // But it's not the correct Deal[].
               // The test ensures that the result of this projection comes from
               // projecting the newly loaded DealResponse, not the existing one
               existingDeals: DealResponse(BorrowerId == deal::BorrowerId) as Deal[] as {
                  name : BorrowerName
                  borrowedAmount : DealAmount
                  id : DealId
               }[]
             }
         }
      """.trimIndent()
         )
            .firstRawObject()

         result.shouldBe(
            mapOf(
               "id" to "1234",
               "borrower" to mapOf(
                  "name" to "Jimmy",
                  "existingDeals" to listOf(
                     mapOf("id" to "d2", "name" to "Jimmy", "borrowedAmount" to 200),
                     mapOf("id" to "d3", "name" to "Jimmy", "borrowedAmount" to 300),
                  )

               )
            ),
         )

         val borrowerIdCallInputs = stub.calls["forBorrowerId"]
            .shouldHaveSize(1)
            .single()
         borrowerIdCallInputs.single().toRawObject().shouldBe("b1")

      }

   @Test
   fun `can use nested constraints to resolve data via an operation`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
model Film {
    @Id
    id: FilmId inherits String
    title: Title inherits String
}

model ReviewData {
    reviewId: ReviewId inherits String
}

model FilmReview {
    reviewId: ReviewId
    rating: StarRating inherits Decimal
}
type CollectionId inherits String
model MovieLibraryResponse {
    films: Film[]
}
service MovieService {
  operation getReviewData(FilmId): ReviewData(...)
  operation getReview(ReviewId): FilmReview(...)
   operation getLibrary(CollectionId): MovieLibraryResponse(...)
}
      """.trimIndent())

      stub.addResponse("getLibrary", """{
  "films": [
    {
      "id": "film-001",
      "title" : "Star Wars"
    },
    {
      "id": "film-002",
      "title" :"Jaws"
    }
  ]
}""")
      stub.addResponse("getReviewData", { _,params ->
         val filmId = params.single().second.toRawObject() as String? ?: error("Stubbing error: No FilmId passed to getReviewData")
         val result = vyne.parseJson("ReviewData", """{ "reviewId" : "review-${filmId.substringAfter("-")}"  }""")
         listOf(result)
      })
      stub.addResponse("getReview", { _,params ->
         val reviewId = params.single().second.toRawObject() as String? ?: error("Stubbing error: No ReviewId passed to getReview")
         val reviews = mapOf("review-001" to 4.0.toBigDecimal(), "review-002" to 3.0.toBigDecimal())
         val reviewResult = vyne.parseJson("FilmReview", """{ "reviewId" : "$reviewId", "rating" : ${reviews[reviewId]} }""")
         listOf(reviewResult)
      })
      val resultUsingTypeReference = vyne.query("""
         given { CollectionId = 'collection-001' }
         find { MovieLibraryResponse } as {
               films: Film[] as (film:Film) -> {
                  title : Title
                  review : FilmReview(ReviewId == ReviewData(FilmId == film::FilmId)::ReviewId) as {
                        reviewId: ReviewId
                        rating: StarRating
                  }
               }[]
         }
      """.trimIndent())
         .firstRawObject()
      val expectedResult = mapOf("films" to listOf(
         mapOf("title" to "Star Wars", "review" to mapOf("reviewId" to "review-001", "rating" to 4.0.toBigDecimal())),
         mapOf("title" to "Jaws", "review" to mapOf("reviewId" to "review-002", "rating" to 3.0.toBigDecimal())),
      ))
      resultUsingTypeReference.shouldBe(expectedResult)


      val resultUsingFieldReference = vyne.query("""
         given { CollectionId = 'collection-001' }
         find { MovieLibraryResponse } as {
               films: Film[] as (film:Film) -> {
                  title : Title
                  // This is the test -- note here we're using a property name, instead of a type reference
                  review : FilmReview(ReviewId == ReviewData(FilmId == film::FilmId).reviewId) as {
                        reviewId: ReviewId
                        rating: StarRating
                  }
               }[]
         }
      """.trimIndent())
         .firstRawObject()
      resultUsingFieldReference.shouldBe(expectedResult)
   }

}
