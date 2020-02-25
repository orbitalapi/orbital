package io.vyne.search.embedded

import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.*
import org.apache.lucene.search.*
import org.apache.lucene.store.BaseDirectory


class SearchIndexRepository(private val directory: BaseDirectory, private val configFactory: ConfigFactory) {
//   private val indexReader: IndexReader
//   private val searcher: IndexSearcher

   private val analyzer: Analyzer

   init {
      // create the writer first, to ensure the index exists before reading
      val initWriter = newWriter()
      initWriter.commit()
      initWriter.close()

      analyzer = configFactory.config().analyzer

//      indexReader = DirectoryReader.open(directory)
//      searcher = IndexSearcher(indexReader)
   }

   private fun newWriter(): IndexWriter {
      return IndexWriter(directory, configFactory.config())
   }

   fun writeAll(documents: List<Document>) {
      val writer = newWriter()
      writer.use { theWriter ->
         theWriter.addDocuments(documents)
         writer.commit()
      }

   }

   fun search(term: String): List<SearchResult> {
      val queryBuilder = BooleanQuery.Builder()
      SearchField.values().forEach { field ->
         queryBuilder.add(FuzzyQuery(Term(field.fieldName, "$term*")), BooleanClause.Occur.SHOULD)
      }
      val query = queryBuilder.build()
      val indexReader = DirectoryReader.open(directory)
      val searcher = IndexSearcher(indexReader)


      val result = searcher.search(query, 10)
      val highlighter = SearchHighlighter.newHighlighter(query)
      return result.scoreDocs.map { hit ->
         val doc = searcher.doc(hit.doc)
         val searchMatches = SearchField.values().mapNotNull { searchField ->

            val fieldContents = doc.getField(searchField.fieldName)?.stringValue() ?: return@mapNotNull null
            highlighter.getBestFragment(analyzer,searchField.fieldName, fieldContents)
               ?.let { highlight -> SearchMatch(searchField,highlight) }
         }
         SearchResult(
            doc.getField(SearchField.QUALIFIED_NAME.fieldName).stringValue().fqn(),
            doc.getField(SearchField.TYPEDOC.fieldName)?.stringValue(),
            searchMatches
         )
      }
   }
}

data class SearchResult(val qualifiedName: QualifiedName, val typeDoc: String?, val matches:List<SearchMatch>)

data class SearchMatch(val field:SearchField, val highlightedMatch:String)

