package io.vyne.cask.ddl

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument
import org.junit.Test

class PostgresDdlGeneratorTest {

    val generator = PostgresDdlGenerator()

    @Test
    fun generatesCreateTable() {
        val (schema, taxi) = schema("""
type Person {
    firstName : FirstName as String
    age : Int
    alive: Boolean
    spouseName : Name? as String
    dateOfBirth: Date
    timestamp: Instant
}""".trim())
        val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema, null, null)
        statement.ddlStatement.should.equal("""
CREATE TABLE IF NOT EXISTS Person_c99239 (
firstName VARCHAR(255) NOT NULL,
age INTEGER NOT NULL,
alive BOOLEAN NOT NULL,
spouseName VARCHAR(255),
dateOfBirth DATE NOT NULL,
timestamp TIMESTAMP NOT NULL
)""".trim())
    }

    @Test
    fun generatesPrimitivesAsColumns() {
        val (_, taxi) = schema("""
type Person {
    firstName : FirstName as String
    age : Int
    alive: Boolean
    spouseName : Name? as String
    dateOfBirth: Date
    timestamp: Instant
}""".trim())
        val person = taxi.objectType("Person")
        generator.generateColumnForField(person.field("firstName")).sql
                .should.equal("firstName VARCHAR(255) NOT NULL")

        generator.generateColumnForField(person.field("age")).sql
                .should.equal("age INTEGER NOT NULL")

        generator.generateColumnForField(person.field("alive")).sql
                .should.equal("alive BOOLEAN NOT NULL")

        generator.generateColumnForField(person.field("spouseName")).sql
                .should.equal("spouseName VARCHAR(255)")

       generator.generateColumnForField(person.field("dateOfBirth")).sql
          .should.equal("dateOfBirth DATE NOT NULL")

       generator.generateColumnForField(person.field("timestamp")).sql
          .should.equal("timestamp TIMESTAMP NOT NULL")

    }

    private fun schema(src: String): Pair<TaxiSchema, TaxiDocument> {
        val schema = TaxiSchema.from(VersionedSource.sourceOnly(src))
        return schema to schema.taxi
    }
}
