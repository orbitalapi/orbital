package io.vyne.connectors.jdbc.query

import com.winterbe.expekt.should
import io.vyne.connectors.SqlTemplateParameter
import io.vyne.connectors.TaxiQlToSqlConverter
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.utils.Ids
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import org.junit.Test

class TaxiQlToSqlConverterTest {

   @Test
   fun `generates simple select from table`() {
      val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         @Table(connection = "testDb", table = "movies", schema = "public")
         model Movie {
            id : MovieId by column("id")
            title : MovieTitle by column("title")
         }""".compiled()
      val query = """findAll { Movie[] }""".query(taxi)
      val queryId = Ids.id("q-")
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query, queryId) { type -> SqlUtils.getTableName(type)}
      sql.should.equal("""select '$queryId' as _queryId, * from movies t0""")
      params.should.be.empty
   }

   @Test
   fun `generates simple select from table with simple where clause`() {
      val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         @Table(connection = "testDb", table = "movies", schema = "public")
         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
      val query = """findAll { Movie[]( MovieTitle == 'Hello' ) }""".query(taxi)
      val queryId = Ids.id("q-")
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query, queryId) { type -> SqlUtils.getTableName(type)}
      sql.should.equal("""select '$queryId' as _queryId, * from movies t0 WHERE t0.title = :title0""")
      params.should.equal(listOf(SqlTemplateParameter("title0", "Hello")))
   }
}


private fun String.query(taxi: TaxiDocument): TaxiQlQuery {
   return Compiler(this, importSources = listOf(taxi)).queries().first()
}

private fun String.compiled(): TaxiDocument {
   val sourceWithImports = """
      ${JdbcConnectorTaxi.Annotations.imports}

      $this
   """.trimIndent()
   return Compiler.forStrings(listOf(JdbcConnectorTaxi.schema, sourceWithImports)).compile()
}
