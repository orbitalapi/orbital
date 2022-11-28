package io.vyne.connectors.jdbc.sql.dml

import com.winterbe.expekt.should
import io.kotest.core.spec.style.DescribeSpec
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlCredentialsConnectionConfiguration
import io.vyne.connectors.jdbc.sqlBuilder
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.toQualifiedName
import org.junit.jupiter.api.Test

class SelectStatementGeneratorTest : DescribeSpec({
   describe("select statement generation") {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
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
         val (sql, params) = SelectStatementGenerator(taxi).toSql(
            query,
            connectionDetails.sqlBuilder()
         ) { type -> type.qualifiedName.toQualifiedName().typeName }
         sql.should.equal("""select * from movie as "t0"""")
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
         val selectStatement = SelectStatementGenerator(taxi).generateSelectSql(query, connectionDetails.sqlBuilder())
         val (sql, params) = selectStatement

         // The odd cast expression here is JOOQ doing it's thing.
         // CAn't work out how to supress it
         val expected = """select * from movie as "t0" where "t0"."title" = cast(:title0 as varchar)"""
         sql.should.equal(expected)
         params.should.equal(listOf(SqlTemplateParameter("title0", "Hello")))
      }
   }


})


private fun String.query(taxi: TaxiDocument): TaxiQlQuery {
   return Compiler(this, importSources = listOf(taxi)).queries().first()
}

private fun String.compiled(): TaxiDocument {
   val sourceWithImports = """

      $this
   """.trimIndent()
   return Compiler.forStrings(listOf(sourceWithImports)).compile()
}

