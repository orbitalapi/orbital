package io.vyne.search.embedded

import com.google.common.base.Stopwatch
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.Type
import io.vyne.utils.log
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.document.Document
import org.apache.lucene.document.TextField
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import org.apache.lucene.document.Field as LuceneField

@Component
class IndexOnStartupTask(private val indexer: SearchIndexer, private val schemaStoreClient: SchemaStoreClient) {
   init {
      log().info("Initializing search, indexing current schema")
      try {
         indexer.createNewIndex(schemaStoreClient.schemaSet())
      } catch (e: IllegalArgumentException) {
         // Thrown by lucene when an index has changed config
         // Lets trash the existing index, and retry
         log().warn("Exception thrown when updating index.  ( ${e.message} ) - will attempt to recover by deleting existing index, and rebuilding")
         indexer.deleteAndRebuildIndex(schemaStoreClient.schemaSet())
      }

   }
}

@Component
class SearchIndexer(private val searchIndexRepository: SearchIndexRepository) {

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changed, re-indexing")
      createNewIndex(event.newSchemaSet)
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
            val camelCaseToWordsName = StringUtils.splitByCharacterTypeCamelCase(searchEntry.name).joinToString(" ")
            add(TextField(SearchField.NAME_AS_WORDS.fieldName, camelCaseToWordsName, LuceneField.Store.YES))
            searchEntry.fieldName?.let { fieldName ->
               add(TextField(SearchField.FIELD_ON_TYPE.fieldName, fieldName, LuceneField.Store.YES))
            }

            searchEntry.typeDoc?.let { typeDoc ->
               add(TextField(SearchField.TYPEDOC.fieldName, typeDoc, LuceneField.Store.YES))
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
         null, // TODO :  Support typeDOc on operations.
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

enum class SearchField(val fieldName: String, val highlightMethod: HighlightMethod = HighlightMethod.HIGHLIGHTER) {
   QUALIFIED_NAME("qualifiedName"),
   NAME("name", highlightMethod = HighlightMethod.SUBSTRING),
   TYPEDOC("typeDoc"),
   FIELD_ON_TYPE(fieldName = "fieldNameOnType"),
   NAME_AS_WORDS("nameAsWords");

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
