package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.drivers.postgres.PostgresDbSupport
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.models.json.parseJson
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyneWithStub
import com.orbitalhq.utils.get
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.BigInteger

@Testcontainers
class PostgresConnectionTests {

   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1").apply {
      withPassword("")
      withInitScript("postgres/actor-schema.sql")
   }
       as PostgreSQLContainer<*>


   @Before
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password

   }

   @Test
   fun `can upsert into Postgres with BigInt Primary Key`(): Unit = runBlocking {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)

      val connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", template, "POSTGRES")))
     val  connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())
      val (vyne, stub) = testVyneWithStub(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${JdbcConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type UpdatedDealAmount inherits Decimal
        type UpdatedDealId inherits Long
        type UpdatedDealDrawdownDate inherits Date

        closed model UpdatedDeal {
            record_id: UpdatedDealId
            Amount: UpdatedDealAmount?
            Drawdown_Date: UpdatedDealDrawdownDate?
        }

        service DealStreamService {
            operation streamUpdatedDeals() : Stream<UpdatedDeal>
        }

        @Table(connection="movies", schema="public", table="deal")
        closed parameter model TracerDeal {
            @Id
            id: UpdatedDealId
            amount: UpdatedDealAmount
            drawdown_date: UpdatedDealDrawdownDate
        }

        @DatabaseService(connection="movies")
        service TracerBulletService {
            @UpsertOperation
            write operation saveDeal(deal: TracerDeal)
        }
        query tracer {
            stream {UpdatedDeal} as {
                id: UpdatedDealId
                amount: UpdatedDealAmount
                drawdown_date: UpdatedDealDrawdownDate
            }[]
            call TracerBulletService::saveDeal
        }
      """
         )
      ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

      stub.addResponse(
         "streamUpdatedDeals", vyne.parseJson(
            "UpdatedDeal[]",
            """[ { "record_id" : 1, "Amount" : 120.12, "Drawdown_Date": "2024-01-01" },
            | { "record_id" : 2, "Amount" : 130.21, "Drawdown_Date": "2024-01-01" }
            | ]""".trimMargin()
         )
      )

      val results = vyne.query("""
                stream {UpdatedDeal} as {
        id: UpdatedDealId
        amount: UpdatedDealAmount
        drawdown_date: UpdatedDealDrawdownDate
    }[]
    call TracerBulletService::saveDeal
            """.trimIndent())
         .rawObjects()


      results.size.shouldBe(2)
      results[0]["amount"].should.equal(BigDecimal("120.12"))
      results[0]["id"].should.equal(BigInteger("1"))
   }

   @Test
   fun `testing connection with valid credentials returns true`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails)
      metadataService.testConnection(PostgresDbSupport.jdbcDriverMetadata.testQuery).get().should.equal(ConnectionSucceeded)
   }

   @Test
   fun `testing connection with valid location but invalid credentials returns false`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials(jdbcUrl, "wrongUser", "wrongPassword")
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails)
      val error = metadataService.testConnection(PostgresDbSupport.jdbcDriverMetadata.testQuery)
         .get()
      error.should.equal("Could not connect to the database: PSQLException : FATAL: role \"wrongUser\" does not exist")
   }

   @Test
   fun `testing connection with invalid location returns false`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials("jdbc:postgresql://wronghost:9999", "wrongUser", "wrongPassword")
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails)
      val error = metadataService.testConnection(PostgresDbSupport.jdbcDriverMetadata.testQuery)
         .get().toString()
      error.trim().should.equal("Could not connect to the database: PSQLException : Unable to parse URL jdbc:postgresql://wronghost:9999")
   }

   @Test
   fun `can list metadata from postgres database`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails)
      metadataService.testConnection(PostgresDbSupport.jdbcDriverMetadata.testQuery).get().should.equal(ConnectionSucceeded)
      val tables = metadataService.listTables()
      tables.should.have.size(2)
      tables.should.contain(
         JdbcTable(
            "public", "actor",
            listOf(
               JdbcColumn(
                  "actor_id",
                  "int4",
                  10,
                  0,
                  false
               )
            ), listOf(
               JdbcIndex(
                  "actor_pkey",
                  listOf(
                     JdbcColumn(
                        "actor_id",
                        "int4",
                        10,
                        0,
                        false
                     )
                  )
               )
            )
         )
      )

      val columns = metadataService.listColumns("public", "actor")
      columns.should.have.size(4)
   }
}
