package com.orbitalhq.connectors.jdbc.sql.mssql.dml

import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.sql.dml.SelectStatementGenerator
import com.orbitalhq.connectors.jdbc.sql.dml.SqlTemplateParameter
import com.orbitalhq.connectors.jdbc.sql.postgres.dml.compiled
import com.orbitalhq.connectors.jdbc.sql.postgres.dml.query
import com.orbitalhq.connectors.jdbc.sql.postgres.dml.shouldBeQueryWithParams
import com.orbitalhq.connectors.jdbc.sqlBuilder
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import lang.taxi.types.toQualifiedName

class MssqlSelectStatementGeneratorTest : DescribeSpec({
   describe("select statement generation") {
     val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "mssql",
         "MSSQL",
         JdbcUrlAndCredentials(
            "jdbc:sqlserver://localhost:14330;database=fake",
            "sa", "PaSSw0rd"
         )
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
         sql.should.equal("""select * from [Movie] [t0]""")
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
         val expected = """select * from [Movie] [t0] where [t0].[title] = :title0"""
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
         val expected = """select * from [movie] [t0] where [t0].[id] = :id0"""
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

         val expected = """select * from [movie] [t0] where [t0].[id] = :id0"""
         sql.should.equal(expected)
         params.should.equal(listOf(SqlTemplateParameter("id0", "123")))
      }

      describe("where clauses") {
         val schema = TaxiSchema.from(
             """
            model Person {
               age : Age inherits Int
            }
         """.trimIndent()
         )
         val taxi = schema.taxi
         val generator = SelectStatementGenerator(schema)
         val dsl = connectionDetails.sqlBuilder()
         it("generates a select for multiple number params") {
            val query = generator.selectSqlWithNamedParams("find { Person[]( Age >= 21 && Age < 40 ) }".query(taxi), dsl)
            query.shouldBeQueryWithParams("""select * from [Person] [t0] where ([t0].[age] >= :age0 and [t0].[age] < :age1)""", listOf(21, 40))
         }
      }
   }


})
