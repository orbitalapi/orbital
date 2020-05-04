package io.vyne.search.embedded

import com.google.common.base.MoreObjects
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.*
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.Highlighter
import org.apache.lucene.store.BaseDirectory


class SearchIndexRepository(private val directory: BaseDirectory, private val configFactory: ConfigFactory) {
//   private val indexReader: IndexReader
//   private val searcher: IndexSearcher

   private val analyzer: Analyzer

   init {
      // create the writer first, to ensure the index exists before reading
      initialize()

      analyzer = configFactory.config().analyzer

//      indexReader = DirectoryReader.open(directory)
//      searcher = IndexSearcher(indexReader)
   }

   private fun initialize() {
      log().info("Initializing search index")
      val initWriter = newWriter()
      initWriter.commit()
      initWriter.close()
   }

   fun destroyAndInitialize() {
      log().info("Destroying existing search indices")
      directory.listAll().forEach { file -> directory.deleteFile(file) }
      initialize()
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
         queryBuilder.add(FuzzyQuery(Term(field.fieldName, "${term.toLowerCase()}*"), 2), BooleanClause.Occur.SHOULD)
         queryBuilder.add(PrefixQuery(Term(field.fieldName, term.toLowerCase())), BooleanClause.Occur.SHOULD)
      }
      val query = queryBuilder.build()
      val indexReader = DirectoryReader.open(directory)
      val searcher = IndexSearcher(indexReader)


      val result = searcher.search(query, 10)
      val highlighter = SearchHighlighter.newHighlighter(query)
      val searchResults = result.scoreDocs.map { hit ->
         val doc = searcher.doc(hit.doc)
         val searchMatches = SearchField.values().mapNotNull { searchField ->

            val fieldContents = doc.getField(searchField.fieldName)?.stringValue() ?: return@mapNotNull null
            when (searchField.highlightMethod) {
               SearchField.HighlightMethod.HIGHLIGHTER -> highlightResult(highlighter, searchField, fieldContents)
               SearchField.HighlightMethod.SUBSTRING -> highlightResultWithSubstring(fieldContents, term, searchField)
            }
         }.distinct()
         SearchResult(
            doc.getField(SearchField.QUALIFIED_NAME.fieldName).stringValue().fqn(),
            doc.getField(SearchField.TYPEDOC.fieldName)?.stringValue(),
            searchMatches
         )
      }

      // Merge the searchResults,
      // as a match can appear on mulitple fields, which seems to return
      // seperate documents.  Odd.
      val distinctSearchResults = searchResults.groupBy { it.qualifiedName }
         .map { (_, results) ->
            results.reduce { acc, searchResult ->
               acc.copy(typeDoc = acc.typeDoc ?: searchResult.typeDoc, matches = acc.matches + searchResult.matches)
            }
         }

      return distinctSearchResults

   }

   private fun highlightResultWithSubstring(fieldContents: String, term: String, searchField: SearchField): SearchMatch? {
      // We use this approach (substring) over highlighting, as for String indexed fields (vs Text indexed fields)
      // with a prefix match, lucene's highlighter matches the entire result, which isn't what
      // the user wants to see.
      if (!fieldContents.toLowerCase().contains(term.toLowerCase())) {
         return null
      }

      val index = fieldContents.indexOf(term, ignoreCase = true)
      val highlightedMatch = fieldContents.substring(0, index) +
         SearchHighlighter.PREFIX +
         fieldContents.substring(index,index + term.length) +
         SearchHighlighter.SUFFIX +
         fieldContents.substring(index + term.length)
      return SearchMatch(
         searchField,
         highlightedMatch
      )

   }

   private fun highlightResult(highlighter: Highlighter, searchField: SearchField, fieldContents: String): SearchMatch? {
      return highlighter.getBestFragment(analyzer, searchField.fieldName, fieldContents)
         ?.let { highlight ->
            SearchMatch(searchField, highlight)
         }
   }
}

data class SearchResult(val qualifiedName: QualifiedName, val typeDoc: String?, val matches: List<SearchMatch>)

data class SearchMatch(val field: SearchField, val highlightedMatch: String)

