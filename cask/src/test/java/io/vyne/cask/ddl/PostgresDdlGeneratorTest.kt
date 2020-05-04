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
}""".trim())
        val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema, null, null)
        statement.ddlStatement.should.equal("""
CREATE TABLE IF NOT EXISTS Person_c99239 (
firstName VARCHAR(255) NOT NULL,
age INTEGER NOT NULL,
alive BOOLEAN NOT NULL,
spouseName VARCHAR(255)
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


    }

    private fun schema(src: String): Pair<TaxiSchema, TaxiDocument> {
        val schema = TaxiSchema.from(VersionedSource.sourceOnly(src))
        return schema to schema.taxi
    }
}
