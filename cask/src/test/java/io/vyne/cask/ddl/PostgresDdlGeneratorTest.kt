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
        val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema)
       val expected = """CREATE TABLE IF NOT EXISTS person_201831 (
"firstName" VARCHAR(255),
"age" INTEGER,
"alive" BOOLEAN,
"spouseName" VARCHAR(255),
"dateOfBirth" DATE,
"timestamp" TIMESTAMP,
"time" TIME,
"caskmessageid" varchar(64),
"cask_raw_id" varchar(64)
,
CONSTRAINT person_201831_pkey PRIMARY KEY ( "cask_raw_id" ));
CREATE INDEX IF NOT EXISTS idx_person_201831_caskmessageid on person_201831("caskmessageid");
""".trim()
       statement.ddlStatement.trim().should.equal(expected)
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
      val statement = generator.generateDdl(schema.versionedType("Person".fqn()), schema)
      val expected = """
CREATE TABLE IF NOT EXISTS person_47dd1f (
"firstName" VARCHAR(255),
"age" INTEGER,
"alive" BOOLEAN,
"spouseName" VARCHAR(255),
"dateOfBirth" DATE,
"timestamp" TIMESTAMP,
"time" TIME,
"caskmessageid" varchar(64)
,
CONSTRAINT person_47dd1f_pkey PRIMARY KEY ( "firstName" ));
CREATE INDEX IF NOT EXISTS idx_person_47dd1f_dateOfBirth ON person_47dd1f("dateOfBirth");
CREATE INDEX IF NOT EXISTS idx_person_47dd1f_caskmessageid on person_47dd1f("caskmessageid");
""".trim()
      statement.ddlStatement.trim().should.equal(expected)
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
}

fun schema(src: String): Pair<TaxiSchema, TaxiDocument> {
   val schema = TaxiSchema.from(VersionedSource.sourceOnly(src))
   return schema to schema.taxi
}
