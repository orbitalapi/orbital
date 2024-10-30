package com.orbitalhq.connectors.jdbc.query

import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.NamedTemplateConnection
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.firstRawObject
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.testVyneWithStub
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.asA
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(
   classes = [JdbcQueryTestConfig::class],
   properties = ["spring.jpa.hibernate.ddl-auto=create", "spring.jpa.show-sql=true", "spring.jpa.properties.hibernate.format_sql=true"]
)
@RunWith(SpringRunner::class)
class JdbcQueryWithJoinsTest {


   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var actorRepository: ActorRepository

   @Autowired
   lateinit var castRepository: CastRepository


   @Autowired
   lateinit var loanRepository: LoanRepository

   @Autowired
   lateinit var repaymentRepo: RepaymentRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory

   @Before
   fun setup() {
      val namedParamTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
      connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", namedParamTemplate, "H2")))
      connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())

      movieRepository.saveAll(
         listOf(
            Movie("1", "A New Hope"),
            Movie("2", "Empire Strikes Back"),
         )
      )
      actorRepository.saveAll(
         listOf(
            Actor(1, "Mark"),
            Actor(2, "Carrie"),
            Actor(3, "Harrison"),

            )
      )
      castRepository.saveAll(
         listOf(
            CastMember(1, actorId = 1, movieId = 1, character = "Luke Skywalker"),
            CastMember(2, actorId = 2, movieId = 1, character = "Princess Leia"),
            CastMember(3, actorId = 3, movieId = 1, character = "Han Solo"),
            CastMember(4, actorId = 1, movieId = 2, character = "Luke Skywalker"),
            CastMember(5, actorId = 2, movieId = 2, character = "Princess Leia"),
            CastMember(6, actorId = 3, movieId = 2, character = "Han Solo"),
            CastMember(7, actorId = 4, movieId = 2, character = "Yoda"),
         )
      )
   }

   val schema = """
       ${JdbcConnectorTaxi.Annotations.imports}
      import ${VyneQlGrammar.QUERY_TYPE_NAME}

      @Table(connection = "movies", table = "MOVIE", schema = "public")
      model Movie {
         @Id
         ID : MovieId inherits Int
         TITLE : MovieTitle inherits String
      }
      @Table(connection = "movies", table = "ACTOR", schema = "public")
      model Actor {
         @Id ID : ActorId inherits Int
         NAME : ActorName inherits String
      }
      @Table(connection = "movies", table = "CAST_MEMBER", schema = "public")
      model CastMember {
         @Id ID : CastMemberId inherits Int
         ACTOR_ID : ActorId
         MOVIE_ID : MovieId
         CHARACTER : Character inherits String
      }

      @DatabaseService( connection = "movies" )
         service MovieDb {
            table movie : Movie[]
            table actor : Actor[]
            table cast : CastMember[]
         }
   """.trimIndent()

   @Test
   fun `joins in nested projection fields`(): Unit = runBlocking {
      loanRepository.saveAll(mutableListOf(
         Loan(loanId = "loan_1", loanApplicationId = "1"),
         Loan(loanId = "loan_2", loanApplicationId = "2"),
      ))

      repaymentRepo.saveAll(mutableListOf(
         Repayment(repaymentId = "repayment_1", loanId = "loan_1"),
         Repayment(repaymentId = "repayment_2", loanId = "loan_2")
      ))


      val schema = """
          ${JdbcConnectorTaxi.Annotations.imports}
          import ${VyneQlGrammar.QUERY_TYPE_NAME}
          type LoanApplicationId inherits String
          type LoanApplicationName inherits String
          model Deal {
            loanApplicationId: LoanApplicationId
            loanApplicationName: LoanApplicationName
          }
          model DealResponse {
            deals : Deal[]
         }

         service Deals {
            operation getDeals():DealResponse
         }

        @Table(connection = "movies", table = "LOAN", schema = "public")
        model Loan {
            @Id
            LOAN_ID : LoanId inherits String
            LOAN_APPLICATION_ID : LoanApplicationId
         }

         @DatabaseService(connection = "movies")
         service LoanService {
             table loans: Loan[]
         }

         @Table(connection = "movies", schema = "public", table = "REPAYMENT" )
         closed model Repayment {
             @Id
             REPAYMENT_ID: RepaymentId inherits String
             LOAN_ID: LoanId
         }

         @DatabaseService(connection = "movies")
         service RepaymentService {
             table repayments: Repayment[]
         }


      """.trimIndent()

      val (vyne, stub) = testVyneWithStub(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            schema
         )
      ) { taxiSchema ->
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(taxiSchema)))
      }

      stub.addResponse(
         "getDeals", vyne.parseJson(
            "DealResponse", """
         { "deals" : [ { "loanApplicationId" : "1", "loanApplicationName" : "deal 1" },  { "loanApplicationId" : "2", "loanApplicationName" : "deal 2" }] }
      """.trimIndent()
         )
      )

      val results = vyne.query("""find { DealResponse } as (Deal[]) -> {
            loanApplicationId : LoanApplicationId
            loanApplicationName : LoanApplicationName
            loan : Loan[] ( LoanApplicationId == Deal::LoanApplicationId ) as (loan:Loan) -> {
               loanId: LoanId
               loanApplicationIdFK: LoanApplicationId
               repayments: Repayment[]( LoanId == Loan::LoanId ) as  (repayments:Repayment) -> {
                  repaymentId: RepaymentId
                  loanIdFK: LoanId
              }[]
            }[]
         }[]
      """.trimIndent())
         .rawObjects()

      results.shouldHaveSize(2)
      val firstLoans = (results[0]["loan"] as List<Map<String, Any?>>)
      val secondLoans = (results[1]["loan"] as List<Map<String, Any?>>)
      firstLoans.size.shouldBe(1)
      secondLoans.size.shouldBe(1)

      val repaymentsForFirstLoan = firstLoans[0]["repayments"] as List<Map<String, Any?>>
      repaymentsForFirstLoan.size.shouldBe(1)
      repaymentsForFirstLoan[0]["loanIdFK"].should.equal("loan_1")

      val repaymentsForSecondLoan = secondLoans[0]["repayments"] as List<Map<String, Any?>>
      repaymentsForSecondLoan.size.shouldBe(1)
      repaymentsForSecondLoan[0]["loanIdFK"].should.equal("loan_2")
   }

   @Test
   fun `can query one-to-many across single join`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            schema
         )
      ) { schema ->
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)))
      }

      val result = vyne.query(
         """find { Movie[] } as (movie:Movie) -> {
         | title : MovieTitle
         | cast : CastMember[](MovieId == movie::MovieId)
         |}[]
      """.trimMargin()
      )
         .typedObjects()
      result.shouldHaveSize(2)

      // If the join was performed correctly, then A New Hope has a cast of 3, and
      // Empire Strikes Back has a cast of 4
      val aNewHope = result.single { it["title"].value == "A New Hope" }
      aNewHope["cast"].asA<TypedCollection>()
         .shouldHaveSize(3)

      val empireStrikesBack = result.single { it["title"].value == "Empire Strikes Back" }
      val empireCast = empireStrikesBack["cast"].asA<TypedCollection>()
      empireCast
         .shouldHaveSize(4)

      // Verify lineage is correct
      val character = empireCast.first().asA<TypedObject>()["CHARACTER"]
      val source = character.source.shouldBeInstanceOf<OperationResultReference>()
      val movieIdParam = source.inputs.single().value.shouldBeInstanceOf<TypeNamedInstance>()
      movieIdParam.value.shouldBe(2)

      // The input of MovieId should have come from the data we loaded from the original query
      // This is the same data source as the one that provided the "title" field.
      val originalQueryDataSourceId = empireStrikesBack["title"].source.asA<OperationResultReference>().remoteCallId
      movieIdParam.dataSourceId.shouldBe(originalQueryDataSourceId)

   }

   @Test
   fun `can query decomposed one-to-many object across single join`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            schema
         )
      ) { schema ->
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)))
      }

      val result = vyne.query(
         """find { Movie[] } as (movie:Movie) -> {
         | title : MovieTitle
         | cast :  CastMember[](MovieId == movie::MovieId) as {
         |  characterName : Character
         | }[]
         |}[]
      """.trimMargin()
      )
         .rawObjects()
      result.shouldBe(
         listOf(
            mapOf(
               "title" to "A New Hope",
               "cast" to listOf(
                  mapOf("characterName" to "Luke Skywalker"),
                  mapOf("characterName" to "Princess Leia"),
                  mapOf("characterName" to "Han Solo"),
               )
            ),
            mapOf(
               "title" to "Empire Strikes Back",
               "cast" to listOf(
                  mapOf("characterName" to "Luke Skywalker"),
                  mapOf("characterName" to "Princess Leia"),
                  mapOf("characterName" to "Han Solo"),
                  mapOf("characterName" to "Yoda"),
               )
            ),
         )
      )
   }

   @Test
   @Ignore("not implemented yet - see ")
   fun `can query one-to-many across multiple join`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            schema
         )
      ) { schema ->
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)))
      }

      val result = vyne.query(
         """find { Movie[] } as (movie:Movie) -> {
         | title : MovieTitle
         | actors : Actor[]( join movie -> Cast -> Actor )
         |}[]
      """.trimMargin()
      )
         .firstRawObject()
      result.shouldNotBeNull()

   }
}

@Entity
data class Loan(
   @Id
   @Column
   val loanId: String,
   @Column
   val loanApplicationId: String
)

interface LoanRepository : JpaRepository<Loan, String>

@Entity
data class Repayment(
   @Id
   @Column
   val repaymentId: String,
   @Column
   val loanId: String
)

interface RepaymentRepository : JpaRepository<Repayment, String>

