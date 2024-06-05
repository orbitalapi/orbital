package com.orbitalhq.connectors.jdbc.sql.mysql.dml

import com.winterbe.expekt.should
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.UpsertVerb
import com.orbitalhq.connectors.jdbc.drivers.databaseSupport
import com.orbitalhq.connectors.jdbc.sql.dml.InsertStatementGenerator
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import org.junit.Test

class InsertStatementGeneratorTest {
   val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
      "mysql",
      JdbcDriver.MYSQL,
      JdbcUrlAndCredentials("jdbc:mysql://localhost:49229/test", "username", "password")
   )

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
         """insert into `Person` (  `firstName`,  `lastName`,  `age`,  `fullName` )
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
insert into `Person` (
  `personId`,
  `firstName`,
  `lastName`,
  `age`,
  `fullName`
)
values (
  123,
  'Jimmy',
  'Schmitts',
  28,
  'Jimmy Schmitts'
)
as `t`
on duplicate key update
  `personId` = `t`.`personId`,
  `firstName` = `t`.`firstName`,
  `lastName` = `t`.`lastName`,
  `age` = `t`.`age`,
  `fullName` = `t`.`fullName`
   """.trimIndent().withoutWhitespace()
      )
   }
}
