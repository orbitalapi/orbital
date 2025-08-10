package com.orbitalhq.connectors.jdbc.sql.postgres.dml

import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.sql.dml.SelectStatementGenerator
import com.orbitalhq.connectors.jdbc.sql.dml.SqlTemplateParameter
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.toQualifiedName

class SelectStatementGeneratorTest : DescribeSpec({
   describe("select statement generation") {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         "POSTGRES",
         JdbcUrlAndCredentials("jdbc:postgresql://localhost:49229/test", "username", "password")
      )

      it("generates simple select from table") {
         val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         model Movie {
            id : MovieId by column("id")
            title : MovieTitle by column("title")
         }""".compiled()
         val query = """find { Movie[] }""".query(taxi)
         val (sql, params) = SelectStatementGenerator(taxi, { type -> type.qualifiedName.toQualifiedName().typeName }).toSql(
            query,
            connectionDetails.sqlBuilder()
         )
         sql.should.equal("""select * from "Movie" as "t0"""")
         params.should.be.empty
      }

      it("generates simple select from table with simple where clause") {
         val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
         val query = """find { Movie[]( MovieTitle == 'Hello' ) }""".query(taxi)
         val selectStatement = SelectStatementGenerator(taxi).selectSqlWithNamedParams(query, connectionDetails.sqlBuilder())
         val (sql, params) = selectStatement

         // The odd cast expression here is JOOQ doing it's thing.
         // CAn't work out how to supress it
         val expected = """select * from "Movie" as "t0" where "t0"."title" = :title0"""
         sql.should.equal(expected)
         params.should.equal(listOf(SqlTemplateParameter("title0", "Hello")))
      }

      it("generates simple select from table with simple where clause using int type") {
         val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         @com.orbitalhq.jdbc.Table(table = "movie", connection = "", schema = "")
         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
         val query = """find { Movie[]( MovieId == 123 ) }""".query(taxi)
         val selectStatement = SelectStatementGenerator(taxi).selectSqlWithNamedParams(query, connectionDetails.sqlBuilder())
         val (sql, params) = selectStatement

         // The odd cast expression here is JOOQ doing it's thing.
         // CAn't work out how to supress it
         val expected = """select * from "movie" as "t0" where "t0"."id" = :id0"""
         sql.should.equal(expected)
         params.should.equal(listOf(SqlTemplateParameter("id0", 123)))
      }

      it("generates simple select from table with simple where clause using String type for Id") {
         val taxi = """
         type MovieId inherits String
         type MovieTitle inherits String

         @com.orbitalhq.jdbc.Table(table = "movie", connection = "", schema = "")
         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
         val query = """find { Movie[]( MovieId == '123' ) }""".query(taxi)
         val selectStatement = SelectStatementGenerator(taxi).selectSqlWithNamedParams(query, connectionDetails.sqlBuilder())
         val (sql, params) = selectStatement

         val expected = """select * from "movie" as "t0" where "t0"."id" = :id0"""
         sql.should.equal(expected)
         params.should.equal(listOf(SqlTemplateParameter("id0", "123")))
      }

      describe("where clauses") {
         val schema = TaxiSchema.from("""
            model Person {
               age : Age inherits Int
            }
         """.trimIndent())
         val taxi = schema.taxi
         val generator = SelectStatementGenerator(schema)
         val dsl = connectionDetails.sqlBuilder()
         it("generates a select for multiple number params") {
            val query = generator.selectSqlWithNamedParams("find { Person[]( Age >= 21 && Age < 40 ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Person" as "t0" where ("t0"."age" >= :age0 and "t0"."age" < :age1)""", listOf(21, 40))
         }
         it("generates a select for IN operator with integer array") {
            val query = generator.selectSqlWithNamedParams("find { Person[]( Age in [21, 25, 30] ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Person" as "t0" where "t0"."age" in (:age0, :age1, :age2)""", listOf(21, 25, 30))
         }
         it("generates a select for NOT IN operator with integer array") {
            val query = generator.selectSqlWithNamedParams("find { Person[]( Age not in [21, 25] ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Person" as "t0" where "t0"."age" not in (:age0, :age1)""", listOf(21, 25))
         }
      }

      describe("IN and NOT IN with string types") {
         val schema = TaxiSchema.from("""
            model Movie {
               title : MovieTitle inherits String
            }
         """.trimIndent())
         val taxi = schema.taxi
         val generator = SelectStatementGenerator(schema)
         val dsl = connectionDetails.sqlBuilder()
         it("generates a select for IN operator with string array") {
            val query = generator.selectSqlWithNamedParams("""find { Movie[]( MovieTitle in ["Star Wars", "Empire", "Jedi"] ) }""".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Movie" as "t0" where "t0"."title" in (:title0, :title1, :title2)""", listOf("Star Wars", "Empire", "Jedi"))
         }
         it("generates a select for NOT IN operator with string array") {
            val query = generator.selectSqlWithNamedParams("""find { Movie[]( MovieTitle not in ["Phantom Menace", "Attack of Clones"] ) }""".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Movie" as "t0" where "t0"."title" not in (:title0, :title1)""", listOf("Phantom Menace", "Attack of Clones"))
         }
      }

      describe("IN and NOT IN with ID types") {
         val schema = TaxiSchema.from("""
            model Product {
               id : ProductId inherits Int
               categoryId : CategoryId inherits Int
               name : ProductName inherits String
            }
         """.trimIndent())
         val taxi = schema.taxi
         val generator = SelectStatementGenerator(schema)
         val dsl = connectionDetails.sqlBuilder()
         it("generates a select for IN operator with ProductId array") {
            val query = generator.selectSqlWithNamedParams("find { Product[]( ProductId in [100, 200, 300] ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Product" as "t0" where "t0"."id" in (:id0, :id1, :id2)""", listOf(100, 200, 300))
         }
         it("generates a select for NOT IN operator with CategoryId array") {
            val query = generator.selectSqlWithNamedParams("find { Product[]( CategoryId not in [1, 5, 9] ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Product" as "t0" where "t0"."categoryId" not in (:categoryId0, :categoryId1, :categoryId2)""", listOf(1, 5, 9))
         }
         it("generates a select for IN operator with single ID value") {
            val query = generator.selectSqlWithNamedParams("find { Product[]( ProductId in [42] ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from "Product" as "t0" where "t0"."id" in (:id0)""", listOf(42))
         }
      }
   }


})


fun Pair<String,List<SqlTemplateParameter>>.shouldBeQueryWithParams(sql: String, params: List<Any>) {
   this.first.should.equal(sql)
   this.second.map { it.value }.should.equal(params)
}
fun String.query(taxi: TaxiDocument): TaxiQlQuery {
   return Compiler(this, importSources = listOf(taxi)).queries().first()
}

fun String.compiled(): TaxiDocument {
   val sourceWithImports = """

      $this
   """.trimIndent()
   return Compiler.forStrings(listOf(sourceWithImports)).compile()
}

