package io.vyne.search.embedded

import com.google.common.base.Stopwatch
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.Type
import io.vyne.utils.log
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field as LuceneField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class IndexOnStartupTask(private val indexer:SearchIndexer, private val schemaStoreClient: SchemaStoreClient) {
   init {
       log().info("Initializing search, indexing current schema")
      indexer.createNewIndex(schemaStoreClient.schemaSet())
   }
}

@Component
class SearchIndexer(private val searchIndexRepository: SearchIndexRepository) {

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changed, re-indexing")
      deleteExistingIndex()
      createNewIndex(event.newSchemaSet)
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

            add(StringField(SearchField.QUALIFIED_NAME.fieldName, searchEntry.qualifiedName, LuceneField.Store.YES))
            add(StringField(SearchField.NAME.fieldName, searchEntry.name, LuceneField.Store.YES))
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
         id,
         SearchEntryType.ATTRIBUTE,
         null, // TODO : should be field.typedoc
         declaringType.fullyQualifiedName
      )
   }

   private fun deleteExistingIndex() {
//      val deleteResult = elastic.bulk(
//         BulkRequest().apply {
//            add(DeleteRequest("types", "*"))
//         }
//         , RequestOptions.DEFAULT)
//      if (deleteResult.hasFailures()) {
//         log().warn("Deleting search index failed after ${deleteResult.took.millis}ms: ${deleteResult.buildFailureMessage()}")
//      } else {
//         log().info("Search index destroyed successfully - ${deleteResult.took.millis}ms")
//      }

   }
}

enum class SearchField(val fieldName:String) {
   QUALIFIED_NAME ("qualifiedName"),
   NAME( "name"),
   TYPEDOC( "typeDoc")
}
enum class SearchEntryType {
   TYPE,
   ATTRIBUTE,
   POLICY,
   SERVICE,
   OPERATION
}

data class SearchEntry(val id: String, val name: String, val qualifiedName: String, val searchEntryType: SearchEntryType, val typeDoc: String?, val declaringEntityQualifiedName: String? = null)
