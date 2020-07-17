package io.vyne.search.embedded

import com.google.common.base.Stopwatch
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStore
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.CompilationException
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.document.Document
import org.apache.lucene.document.TextField
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import org.apache.lucene.document.Field as LuceneField

@Component
class IndexOnStartupTask(private val indexer: SearchIndexer, private val schemaStore: SchemaStore) {
   init {
      log().info("Initializing search, indexing current schema")
      try {
         indexer.createNewIndex(schemaStore.schemaSet())
      } catch (e: IllegalArgumentException) {
         // Thrown by lucene when an index has changed config
         // Lets trash the existing index, and retry
         log().warn("Exception thrown when updating index.  ( ${e.message} ) - will attempt to recover by deleting existing index, and rebuilding")
         indexer.deleteAndRebuildIndex(schemaStore.schemaSet())
      } catch (e:CompilationException) {
         log().warn("Compilation exception found when trying to create search indexes on startup - we'll just wait. \n ${e.message}")
      }

   }
}

@Component
class SearchIndexer(private val searchIndexRepository: SearchIndexRepository) {

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changed, re-indexing")
      deleteAndRebuildIndex(event.newSchemaSet)
   }

   internal fun deleteAndRebuildIndex(schemaSet: SchemaSet) {
      searchIndexRepository.destroyAndInitialize()
      createNewIndex(schemaSet)
   }

   internal fun createNewIndex(schemaSet: SchemaSet) {
      val stopwatch = Stopwatch.createStarted()
      val searchEntries = schemaSet.schema.types.flatMap { searchIndexEntry(it) } +
         schemaSet.schema.operations.map { searchIndexEntry(it) }

      if (searchEntries.isEmpty()) {
         log().warn("No members in the schema, so not creating search entries")
         return
      }

      val searchDocs = searchEntries.map { searchEntry ->
         Document().apply {

            add(TextField(SearchField.QUALIFIED_NAME.fieldName, searchEntry.qualifiedName, LuceneField.Store.YES))
            add(TextField(SearchField.NAME.fieldName, searchEntry.name, LuceneField.Store.YES))
            val camelCaseToWordsName = StringUtils
               .splitByCharacterTypeCamelCase(searchEntry.name)
               // searchEntry.name is already the value of SearchField.NAME.fieldName field. See below comment.
               .filterNot { it != searchEntry.name }
               .joinToString(" ")
            add(TextField(SearchField.NAME_AS_WORDS.fieldName, camelCaseToWordsName, LuceneField.Store.YES))
             /*
             FieldName always equals to name when populated. As we search against all fields (i.e. we add a query for each field
             in the corresponding QueryBuilder, fields with same values not only increases query time without providing any value but also
             causing too much noise in search result with duplicates.
             searchEntry.fieldName?.let { fieldName ->
               add(TextField(SearchField.FIELD_ON_TYPE.fieldName, fieldName, LuceneField.Store.YES))
            }
            */
            if (!searchEntry.typeDoc.isNullOrBlank()) {
               add(TextField(SearchField.TYPEDOC.fieldName, searchEntry.typeDoc, LuceneField.Store.YES))
            }

         }
      }
      searchIndexRepository.writeAll(searchDocs)
      val elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS)
      log().info("Created search index with ${searchDocs.size} entries in $elapsed ms")
   }

   private fun searchIndexEntry(operation: Operation): SearchEntry {
      return SearchEntry(
         operation.qualifiedName.fullyQualifiedName,
         operation.name,
         operation.qualifiedName.fullyQualifiedName,
         SearchEntryType.OPERATION,
         operation.typeDoc,// TODO :  Support typeDOc on operations.
         null // Todo : Strictly speaking, this should be the service
      )
   }

   private fun searchIndexEntry(type: Type): List<SearchEntry> {
      return listOf(SearchEntry(type.name.parameterizedName,
         type.name.name,
         type.name.parameterizedName,
         SearchEntryType.TYPE,
         type.typeDoc)
      ) + type.attributes.map { (name, field) -> searchIndexEntry(type, name, field) }
   }

   private fun searchIndexEntry(declaringType: Type, name: String, field: Field): SearchEntry {
      val id = declaringType.fullyQualifiedName + "#Field[$name]"
      return SearchEntry(
         id,
         name,
         declaringType.fullyQualifiedName,
         SearchEntryType.ATTRIBUTE,
         field.typeDoc,
         name
      )
   }

}

enum class SearchField(val fieldName: String, val highlightMethod: HighlightMethod = HighlightMethod.HIGHLIGHTER, val boostFactor: Float = 1.0f) {
   QUALIFIED_NAME("qualifiedName",  HighlightMethod.SUBSTRING, 1.5f),
   NAME("name", HighlightMethod.SUBSTRING, 3.0f),
   TYPEDOC("typeDoc", HighlightMethod.HIGHLIGHTER, 1f),
   FIELD_ON_TYPE("fieldNameOnType", HighlightMethod.HIGHLIGHTER, 1.2f),
   NAME_AS_WORDS("nameAsWords", HighlightMethod.HIGHLIGHTER, 0.7f);

   enum class HighlightMethod {
      SUBSTRING,
      HIGHLIGHTER;
   }
}

enum class SearchEntryType {
   TYPE,
   ATTRIBUTE,
   POLICY,
   SERVICE,
   OPERATION
}

data class SearchEntry(
   val id: String,
   val name: String,
   val qualifiedName: String,
   val searchEntryType: SearchEntryType,
   val typeDoc: String?,
   val fieldName: String? = null
)
