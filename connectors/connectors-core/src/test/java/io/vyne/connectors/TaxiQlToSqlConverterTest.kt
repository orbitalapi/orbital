package io.vyne.connectors

import com.winterbe.expekt.should
import lang.taxi.Compiler
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.toQualifiedName
import org.junit.Test

class TaxiQlToSqlConverterTest {

   @Test
   fun `generates simple select from table`() {
      val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         model Movie {
            id : MovieId by column("id")
            title : MovieTitle by column("title")
         }""".compiled()
      val query = """find { Movie[] }""".query(taxi)
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query) { type -> type.qualifiedName.toQualifiedName().typeName  }
      sql.should.equal("""select * from Movie t0""")
      params.should.be.empty
   }

   @Test
   fun `generates simple select from table with simple where clause`() {
      val taxi = """
         type MovieId inherits Int
         type MovieTitle inherits String

         model Movie {
            id : MovieId
            title : MovieTitle
         }""".compiled()
      val query = """find { Movie[]( MovieTitle == 'Hello' ) }""".query(taxi)
      val (sql, params) = TaxiQlToSqlConverter(taxi).toSql(query) { type -> type.qualifiedName.toQualifiedName().typeName  }
      sql.should.equal("""select * from Movie t0 WHERE t0.title = :title0""")
      params.should.equal(listOf(SqlTemplateParameter("title0", "Hello")))
   }
}


private fun String.query(taxi: TaxiDocument): TaxiQlQuery {
   return Compiler(this, importSources = listOf(taxi)).queries().first()
}

private fun String.compiled(): TaxiDocument {
   val sourceWithImports = """

      $this
   """.trimIndent()
   return Compiler.forStrings(listOf(sourceWithImports)).compile()
}

