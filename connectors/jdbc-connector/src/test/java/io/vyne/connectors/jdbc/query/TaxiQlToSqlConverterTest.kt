package io.vyne.connectors.jdbc.query

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.SqlTemplateParameter
import io.vyne.connectors.jdbc.Taxi
import io.vyne.connectors.jdbc.TaxiQlToSqlConverter
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

         @Table(name = "movies")
         model Movie {
            id : MovieId by column("id")
            title : MovieTitle by column("title")
         }""".compiled()
      val query = """findAll { Movie[] }""".query(taxi)
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query)
      sql.should.equal("""select * from movies t0""")
      params.should.be.empty
   }

   @Test
   fun `generates simple select from table with simple where clause`() {
      val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         @Table(name = "movies")
         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
      val query = """findAll { Movie[]( MovieTitle = 'Hello' ) }""".query(taxi)
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query)
      sql.should.equal("""select * from movies t0 WHERE t0.title = :title0""")
      params.should.equal(listOf(SqlTemplateParameter("title0", "Hello")))
   }
}


private fun String.query(taxi: TaxiDocument): TaxiQlQuery {
   return Compiler(this, importSources = listOf(taxi)).queries().first()
}

private fun String.compiled(): TaxiDocument {
   val sourceWithImports = """
      ${Taxi.Annotations.imports}

      $this
   """.trimIndent()
   return Compiler.forStrings(listOf(Taxi.schema, sourceWithImports)).compile()
}
