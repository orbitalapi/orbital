package com.orbitalhq.connectors.jdbc.sql.postgres.dml

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.databaseSupport
import com.orbitalhq.connectors.jdbc.sql.dml.InsertStatementGenerator
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import com.winterbe.expekt.should
import org.junit.Test

class InsertStatementGeneratorTest {
   val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
      "postgres",
      JdbcDriver.POSTGRES,
      JdbcUrlAndCredentials("jdbc:postgresql://localhost:49229/test", "username", "password")
   )

    @Test
    fun `can insert typed instance to db table with BigInt Primary Key`() {
        val schema = TaxiSchema.from(
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
        
      
        @com.orbitalhq.jdbc.Table(schema = "public", table = "TracerDeal", connection = "postgres")
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

        val typedInstance = TypedInstance.from(
            schema.type("TracerDeal"),
            """{ "id" : 123, "amount" : 2500.25, "drawdown_date" : "2024-01-01" }""",
            schema
        )
        val insert = InsertStatementGenerator(schema, connectionDetails.databaseSupport).generateInsertWithoutConnecting(typedInstance, connectionDetails, UpsertVerb.Upsert)
        val sql = insert.toString()
        sql.withoutWhitespace().should.equal(
            """insert into "TracerDeal" ("id", "amount", "drawdown_date")
values (
  123, 
  2500.25, 
  date '2024-01-01'
)
on conflict ("id")
do update
set
  "id" = excluded."id",
  "amount" = excluded."amount",
  "drawdown_date" = excluded."drawdown_date"
            """.trimIndent().withoutWhitespace()
        )
    }
   @Test
   fun `can insert typed instance to db table`() {
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "public", table = "Person", connection = "postgres")
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
            age : Age inherits Int
            fullName : FullName inherits String by concat(this.firstName, ' ', this.lastName)
         }
      """.trimIndent()
      )
      val typedInstance = TypedInstance.from(
         schema.type("Person"),
         """{ "firstName" : "Jimmy", "lastName" : "Schmitts", "age" : 28 }""",
         schema
      )
      val insert = InsertStatementGenerator(schema, connectionDetails.databaseSupport).generateInsertWithoutConnecting(typedInstance, connectionDetails, UpsertVerb.Upsert)
      val sql = insert.toString()
      sql.withoutWhitespace().should.equal(
         """insert into "Person" (  "firstName",  "lastName",  "age",  "fullName" )
            values (   'Jimmy',  'Schmitts',   28,    'Jimmy Schmitts')""".withoutWhitespace()
      )
   }

   @Test
   fun `can upsert typed instance to db table`() {
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "public", table = "Person", connection = "postgres")
         model Person {
            @Id
            personId : PersonId inherits Int
            @Id
            firstName : FirstName inherits String
            lastName : LastName inherits String
            age : Age inherits Int
            fullName : FullName inherits String by concat(this.firstName, ' ', this.lastName)
         }
      """.trimIndent()
      )
      val typedInstance = TypedInstance.from(
         schema.type("Person"),
         """{ "personId" : 123, "firstName" : "Jimmy", "lastName" : "Schmitts", "age" : 28 }""",
         schema
      )
      val insert = InsertStatementGenerator(schema, connectionDetails.databaseSupport).generateInsertWithoutConnecting(
         typedInstance,
         connectionDetails,
         UpsertVerb.Upsert
      )
      val sql = insert.toString()
      sql.withoutWhitespace().should.equal(
"""
insert into "Person" (
  "personId",
  "firstName",
  "lastName",
  "age",
  "fullName"
)
values (
  123,
  'Jimmy',
  'Schmitts',
  28,
  'Jimmy Schmitts'
)
on conflict ("personId", "firstName")
do update
set
  "personId" = excluded."personId",
  "firstName" = excluded."firstName",
  "lastName" = excluded."lastName",
  "age" = excluded."age",
  "fullName" = excluded."fullName"
   """.trimIndent().withoutWhitespace()
      )
   }
}
