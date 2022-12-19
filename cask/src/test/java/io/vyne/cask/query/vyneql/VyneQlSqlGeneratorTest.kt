package io.vyne.cask.query.vyneql

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class VyneQlSqlGeneratorTest {
   val schemaProvider = SimpleTaxiSchemaProvider(
      """
      type FirstName inherits String
      type Age inherits Int
      type LoginTime inherits Instant
      type LastLoggedIn inherits LoginTime
      type BaseDate inherits Date
      type InheritedDate inherits BaseDate
      type BirthDate inherits InheritedDate
      model Person {
         firstName : FirstName
         age : Age
         lastLogin : LastLoggedIn
         birthDate: BirthDate
      }
   """
   )

   lateinit var caskConfigRepository: CaskConfigRepository
   lateinit var sqlGenerator: VyneQlSqlGenerator

   @Before
   fun setup() {
      caskConfigRepository = mock {
         on { findAllByQualifiedTypeNameAndStatus(any(), any()) } doReturn listOf(
            CaskConfig(
               tableName = "person",
               qualifiedTypeName = "Person",
               versionHash = "abcdef",
               insertedAt = Instant.now()
            )
         )
      }
      sqlGenerator = VyneQlSqlGenerator(schemaProvider, caskConfigRepository)
   }

   @Test
   fun generatesSqlForFindAllWithoutArgs() {
      val statement = sqlGenerator.generateSql("find { Person[] }")
      statement.shouldEqual("""select * from person as "t0"""", emptyList())
   }

   @Test
   fun generatesSqlForFindByStringArg() {
      val statement = sqlGenerator.generateSql("find { Person[]( FirstName == 'Jimmy' ) }")
      statement.shouldEqual("""select * from person as "t0" where "t0"."firstName" = ?""", listOf("Jimmy"))
   }

   @Test
   fun generatesSqlForFindByNumberArg() {
      val statement = sqlGenerator.generateSql("find { Person[]( Age == 21 ) }")
      statement.shouldEqual("""select * from person as "t0" where "t0"."age" = ?""", listOf(21))
   }

   @Test
   fun generatesSqlForFindBetweenNumberArg() {
      val statement = sqlGenerator.generateSql("find { Person[]( Age >= 21 && Age < 40 ) }")
      statement.shouldEqual("""select * from person as "t0" where ("t0"."age" >= ? and "t0"."age" < ?)""", listOf(21, 40))
   }

   @Test
   fun `params that are date time strings are parsed to instant`() {
      val statement = sqlGenerator.generateSql("find { Person[]( LastLoggedIn >= '2020-11-10T15:00:00Z' ) }")
      statement.shouldEqual(
         """select * from person as "t0" where "t0"."lastLogin" >= cast(? as timestamp with time zone)""",
         listOf(OffsetDateTime.parse("2020-11-10T15:00:00Z"))
      )
   }

   @Test
   fun `date time between params are parsed correctly`() {
      val statement =
         sqlGenerator.generateSql("""find { Person[]( LastLoggedIn >= "2020-10-10T15:00:00Z" &&  LastLoggedIn < "2020-11-10T15:00:00Z"  ) }""")
      statement.shouldEqual(
         """select * from person as "t0" where ("t0"."lastLogin" >= cast(? as timestamp with time zone) and "t0"."lastLogin" < cast(? as timestamp with time zone))""",
         listOf(OffsetDateTime.parse("2020-10-10T15:00:00Z"), OffsetDateTime.parse("2020-11-10T15:00:00Z"))
      )
   }

   @Test
   fun `when querying using a base type, the field is resolved correctly`() {
      val statement =
         sqlGenerator.generateSql("""find { Person[]( LoginTime >= "2020-10-10T15:00:00Z" &&  LoginTime < "2020-11-10T15:00:00Z"  ) }""")
      statement.shouldEqual(
         """select * from person as "t0" where ("t0"."lastLogin" >= cast(? as timestamp with time zone) and "t0"."lastLogin" < cast(? as timestamp with time zone))""",
         listOf(OffsetDateTime.parse("2020-10-10T15:00:00Z"), OffsetDateTime.parse("2020-11-10T15:00:00Z"))
      )
   }

   @Test
   fun `when querying using a grand-base type, the field is resolved correctly`() {
      val statement =
         sqlGenerator.generateSql("""find { Person[]( BaseDate >= "2020-10-10" &&  BaseDate < "2020-11-10"  ) }""")
      statement.shouldEqual(
         """select * from person as "t0" where ("t0"."birthDate" >= cast(? as date) and "t0"."birthDate" < cast(? as date))""",
         listOf(LocalDate.parse("2020-10-10"), LocalDate.parse("2020-11-10"))
      )
   }

   // Jooq is adding cast syntax for timestamp values.
   // There doesn't appear to be a way to stop it, as params have already been
   // correctly cast to the appropriate sql type.
   private val timestampParam = "cast(? as timestamp with time zone)"


   private fun SqlStatement.shouldEqual(sql: String, params: List<Any>) {
      println(this.sql)
      this.sql.shouldBeEqualIgnoringCase(sql)
      this.params.should.equal(params)
   }
}
