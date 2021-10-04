package io.vyne.search.embedded

import io.vyne.query.graph.Algorithms
import io.vyne.query.graph.OperationQueryResultItemRole
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.utils.log
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.highlight.Highlighter
import org.springframework.stereotype.Component

@Component
class SearchIndexRepository(
   private val indexWriter:IndexWriter,
   private val searchManager: SearcherManager,
   private val configFactory: ConfigFactory) {
   fun destroyAndInitialize() {
      log().info("Destroying existing search indices")
      indexWriter.deleteAll()
      indexWriter.commit()
   }

   fun writeAll(documents: List<Document>) {
      indexWriter.addDocuments(documents)
      indexWriter.commit()
   }

   fun search(term: String, schema: Schema): List<SearchResult> {
      searchManager.maybeRefresh()
      val queryBuilder = BooleanQuery.Builder()
      SearchField.values().forEach { field ->
         // Decreasing the score of fuzzy search as it can produce a match for 'time' when we search for 'fixe'
         queryBuilder.add(BoostQuery(FuzzyQuery(Term(field.fieldName, "${term.toLowerCase()}*"), 2),  field.boostFactor * 0.11F), BooleanClause.Occur.SHOULD)
         queryBuilder.add(BoostQuery(PrefixQuery(Term(field.fieldName, term.toLowerCase())),  field.boostFactor), BooleanClause.Occur.SHOULD)
      }
      val query = queryBuilder.build()
      val searcher = searchManager.acquire()

      val result = searcher.search(query, 1000)
      val highlighter = SearchHighlighter.newHighlighter(query)
      val searchResults = result.scoreDocs.map { hit ->
         val doc = searcher.doc(hit.doc)
         val searchMatches = SearchField.values().mapNotNull { searchField ->

            val fieldContents = doc.getField(searchField.fieldName)?.stringValue() ?: return@mapNotNull null
            when (searchField.highlightMethod) {
               SearchField.HighlightMethod.HIGHLIGHTER -> highlightResult(highlighter, searchField, fieldContents)
               SearchField.HighlightMethod.SUBSTRING -> highlightResultWithSubstring(fieldContents, term, searchField)
               SearchField.HighlightMethod.NONE -> null
            }
         }.distinct()
         val searchResultFullyQualifiedName = doc.getField(SearchField.QUALIFIED_NAME.fieldName).stringValue().fqn()
         val vyneType = schema.type(searchResultFullyQualifiedName)
         val operationQueryResult = Algorithms.findAllFunctionsWithArgumentOrReturnValueForType(
            schema,
            searchResultFullyQualifiedName.fullyQualifiedName)

         SearchResult(
            searchResultFullyQualifiedName,
            doc.getField(SearchField.TYPEDOC.fieldName)?.stringValue(),
            doc.getField(SearchField.FIELD_ON_TYPE.fieldName)?.stringValue(),
            searchMatches,
            SearchEntryType.fromName(doc.getField(SearchField.MEMBER_TYPE.fieldName)?.stringValue()),
            hit.score,
            consumers = operationQueryResult.results.filter { it.role == OperationQueryResultItemRole.Input }.map { it.operationName },
            producers = operationQueryResult.results.filter { it.role == OperationQueryResultItemRole.Output }.map { it.operationName },
            metadata = vyneType.metadata
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

      return distinctSearchResults.sortedByDescending { it.score }

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
      return highlighter.getBestFragment(configFactory.config().analyzer, searchField.fieldName, fieldContents)
         ?.let { highlight ->
            SearchMatch(searchField, highlight)
         }
   }
}

data class SearchResult(
   val qualifiedName: QualifiedName,
   val typeDoc: String?,
   val matchedFieldName: String?,
   val matches: List<SearchMatch>,
   val memberType:SearchEntryType,
   val score: Float,
   val consumers: List<QualifiedName> = emptyList(),
   val producers: List<QualifiedName> = emptyList(),
   val metadata: List<io.vyne.schemas.Metadata> = emptyList()
)

data class SearchMatch(val field: SearchField, val highlightedMatch: String)

