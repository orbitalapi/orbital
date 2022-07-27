package io.vyne.connectors.jdbc.sql.dml

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlCredentialsConnectionConfiguration
import io.vyne.models.TypedInstance
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import org.junit.Test

class InsertStatementGeneratorTest {
   val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
      "postgres",
      JdbcDriver.POSTGRES,
      JdbcUrlAndCredentials("jdbc:postgresql://localhost:49229/test", "username", "password")
   )

   @Test
   fun `can insert typed instance to db table`() {
      val schema = TaxiSchema.from(
         """
         @io.vyne.jdbc.Table(schema = "public", table = "Person", connection = "postgres")
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
      val insert = InsertStatementGenerator(schema).generateInsertWithoutConnecting(typedInstance, connectionDetails)
      val sql = insert.toString()
      sql.withoutWhitespace().should.equal(
         """insert into Person (  firstName,  lastName,  age,  fullName )
            values (   'Jimmy',  'Schmitts',   28,    'Jimmy Schmitts')""".withoutWhitespace()
      )
   }

   @Test
   fun `can upsert typed instance to db table`() {
      val schema = TaxiSchema.from(
         """
         @io.vyne.jdbc.Table(schema = "public", table = "Person", connection = "postgres")
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
      val insert = InsertStatementGenerator(schema).generateInsertWithoutConnecting(
         typedInstance,
         connectionDetails,
         useUpsertSemantics = true
      )
      val sql = insert.toString()
      sql.withoutWhitespace().should.equal(
         """insert into Person (  personId, firstName, lastName, age, fullName )
values ( 123, 'Jimmy', 'Schmitts', 28, 'Jimmy Schmitts')
on conflict (personId, firstName) do update
set lastName = 'Schmitts',
    age = 28,
    fullName = 'Jimmy Schmitts'""".withoutWhitespace()
      )
   }
}
