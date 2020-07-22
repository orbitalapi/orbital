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
    @Something
    age : Int
    alive: Boolean
    spouseName : Name? as String
    @Something
    dateOfBirth: Date
    timestamp: Instant
    time: Time
}""".trim())
        val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema, null, null)
        statement.ddlStatement.trim().should.equal("""
CREATE TABLE IF NOT EXISTS Person_201831 (
"firstName" VARCHAR(255),
"age" INTEGER,
"alive" BOOLEAN,
"spouseName" VARCHAR(255),
"dateOfBirth" DATE,
"timestamp" TIMESTAMP,
"time" TIME);
""".trim())
    }

   @Test
   fun generatesCreateTableWithPk() {
      val (schema, taxi) = schema("""
type Person {
    @PrimaryKey
    @Indexed
    firstName : FirstName as String
    @Something
    age : Int
    alive: Boolean
    spouseName : Name? as String
    @Indexed
    @Something
    dateOfBirth: Date
    timestamp: Instant
    time: Time
}""".trim())
      val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema, null, null)
      statement.ddlStatement.should.equal("""
CREATE TABLE IF NOT EXISTS Person_47dd1f (
"firstName" VARCHAR(255),
"age" INTEGER,
"alive" BOOLEAN,
"spouseName" VARCHAR(255),
"dateOfBirth" DATE,
"timestamp" TIMESTAMP,
"time" TIME,
CONSTRAINT Person_47dd1f_pkey PRIMARY KEY ( "firstName" ));
CREATE INDEX IF NOT EXISTS idx_Person_47dd1f_dateOfBirth ON Person_47dd1f("dateOfBirth");""".trim())
   }

    @Test
    fun generatesPrimitivesAsColumns() {
        val (_, taxi) = schema("""
enum Gender {
   Male,
   Female
}
enum GenderType {
   Male synonym of Gender.Male
}
enum OriginalGender inherits Gender
type Person {
    firstName : FirstName as String
    age : Int
    alive: Boolean
    spouseName : Name? as String
    dateOfBirth: Date
    timestamp: Instant
    gender : GenderType
    originalGender : OriginalGender
    time: Time
}""".trim())
        val person = taxi.objectType("Person")
        generator.generateColumnForField(person.field("firstName")).sql
                .should.equal(""""firstName" VARCHAR(255)""")

        generator.generateColumnForField(person.field("age")).sql
                .should.equal(""""age" INTEGER""")

        generator.generateColumnForField(person.field("alive")).sql
                .should.equal(""""alive" BOOLEAN""")

        generator.generateColumnForField(person.field("spouseName")).sql
                .should.equal(""""spouseName" VARCHAR(255)""")

       generator.generateColumnForField(person.field("dateOfBirth")).sql
          .should.equal(""""dateOfBirth" DATE""")

       generator.generateColumnForField(person.field("timestamp")).sql
          .should.equal(""""timestamp" TIMESTAMP""")

       generator.generateColumnForField(person.field("gender")).sql
          .should.equal(""""gender" VARCHAR(255)""")

       generator.generateColumnForField(person.field("originalGender")).sql
          .should.equal(""""originalGender" VARCHAR(255)""")

       generator.generateColumnForField(person.field("time")).sql
          .should.equal(""""time" TIME""")
    }

    private fun schema(src: String): Pair<TaxiSchema, TaxiDocument> {
        val schema = TaxiSchema.from(VersionedSource.sourceOnly(src))
        return schema to schema.taxi
    }
}
