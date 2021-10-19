package io.vyne.cask.query.vyneql

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class VyneQlSqlGeneratorTest {
   val schemaProvider = SimpleTaxiSchemaProvider("""
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
   """)

   lateinit var caskConfigRepository: CaskConfigRepository
   lateinit var sqlGenerator: VyneQlSqlGenerator

   @Before
   fun setup() {
      caskConfigRepository = mock {
         on { findAllByQualifiedTypeNameAndStatus(any(),any()) } doReturn listOf(CaskConfig(
            tableName = "person",
            qualifiedTypeName = "Person",
            versionHash = "abcdef",
            insertedAt = Instant.now()
         ))
      }
      sqlGenerator = VyneQlSqlGenerator(schemaProvider, caskConfigRepository)
   }

   @Test
   fun generatesSqlForFindAllWithoutArgs() {
      val statement = sqlGenerator.generateSql("findAll { Person[] }")
      statement.shouldEqual("SELECT * from person;", emptyList())
   }

   @Test
   fun generatesSqlForFindByStringArg() {
      val statement = sqlGenerator.generateSql("findAll { Person[]( FirstName == 'Jimmy' ) }")
      statement.shouldEqual("""SELECT * from person WHERE "firstName" = ?;""", listOf("Jimmy"))
   }
   @Test
   fun generatesSqlForFindByNumberArg() {
      val statement = sqlGenerator.generateSql("findAll { Person[]( Age == 21 ) }")
      statement.shouldEqual("""SELECT * from person WHERE "age" = ?;""", listOf(21))
   }

   @Test
   fun generatesSqlForFindBetweenNumberArg() {
      val statement = sqlGenerator.generateSql("findAll { Person[]( Age >= 21, Age < 40 ) }")
      statement.shouldEqual("""SELECT * from person WHERE "age" >= ? AND "age" < ?;""", listOf(21, 40))
   }

   @Test
   fun `params that are date time strings are parsed to instant`() {
      val statement = sqlGenerator.generateSql("findAll { Person[]( LastLoggedIn >= '2020-11-10T15:00:00Z' ) }")
      statement.shouldEqual("""SELECT * from person WHERE "lastLogin" >= ?;""", listOf(LocalDateTime.parse("2020-11-10T15:00:00")))
   }

   @Test
   fun `date time between params are parsed correctly`() {
      val statement = sqlGenerator.generateSql("""findAll { Person[]( LastLoggedIn >= "2020-10-10T15:00:00Z",  LastLoggedIn < "2020-11-10T15:00:00Z"  ) }""")
      statement.shouldEqual("""SELECT * from person WHERE "lastLogin" >= ? AND "lastLogin" < ?;""", listOf(LocalDateTime.parse("2020-10-10T15:00:00"), LocalDateTime.parse("2020-11-10T15:00:00")))
   }

   @Test
   fun `when querying using a base type, the field is resolved correctly`() {
      val statement = sqlGenerator.generateSql("""findAll { Person[]( LoginTime >= "2020-10-10T15:00:00Z",  LoginTime < "2020-11-10T15:00:00Z"  ) }""")
      statement.shouldEqual("""SELECT * from person WHERE "lastLogin" >= ? AND "lastLogin" < ?;""", listOf(LocalDateTime.parse("2020-10-10T15:00:00"), LocalDateTime.parse("2020-11-10T15:00:00")))
   }

   @Test
   fun `when querying using a grand-base type, the field is resolved correctly`() {
      val statement = sqlGenerator.generateSql("""findAll { Person[]( BaseDate >= "2020-10-10",  BaseDate < "2020-11-10"  ) }""")
      statement.shouldEqual("""SELECT * from person WHERE "birthDate" >= ? AND "birthDate" < ?;""", listOf(LocalDate.parse("2020-10-10"), LocalDate.parse("2020-11-10")))
   }


   @Test
   fun generatesSqlForFindAllWithoutArgsWithFilterSQL() {
      val filterSQL = "ABC in ('XYZ')"
      val statement = sqlGenerator.generateSql("findAll { Person[] }", filterSQL)
      statement.shouldEqual("SELECT * from person WHERE ABC in ('XYZ');", emptyList())
   }

   @Test
   fun generatesSqlForFindByStringArgWithFilterSQL() {
      val filterSQL = "ABC in ('XYZ')"
      val statement = sqlGenerator.generateSql("findAll { Person[]( FirstName == 'Jimmy' ) }", filterSQL)
      statement.shouldEqual("""SELECT * from person WHERE "firstName" = ? AND ABC in ('XYZ');""", listOf("Jimmy"))
   }

   private fun SqlStatement.shouldEqual(sql: String, params: List<Any>) {
      this.sql.should.equal(sql)
      this.params.should.equal(params)
   }
}
