package io.vyne.search.elastic

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.schemaStore.SchemaSet
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemas.Type
import io.vyne.utils.log
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import sun.misc.PerformanceLogger

@Component
class SearchIndexer(val elastic: RestHighLevelClient, val objectMapper: ObjectMapper) {

   @EventListener
   fun onSchemaSetChanged(event: SchemaSetChangedEvent) {
      log().info("Schema set changed, re-indexing")
      deleteExistingIndex()
      createNewIndex(event.newSchemaSet)
   }

   private fun createNewIndex(schemaSet: SchemaSet) {
      val searchEntries = schemaSet.schema.types.flatMap { searchIndexEntry(it) } +
         schemaSet.schema.operations.map { searchIndexEntry(it) }

      if (searchEntries.isEmpty()) {
         log().warn("No members in the schema, so not creating search entries")
         return
      }
      val request = BulkRequest()
      searchEntries.forEach {
         request.add(IndexRequest("types").id(it.id).source(objectMapper.writeValueAsString(it)))
      }
      val result = elastic.bulk(request, RequestOptions.DEFAULT)
      if (result.hasFailures()) {
         log().warn("Creating search index failed after ${result.took.millis}ms: ${result.buildFailureMessage()}")
      } else {
         log().info("Search index created successfully in ${result.took.millis}ms")
      }
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
      val deleteResult = elastic.bulk(
         BulkRequest().apply {
            add(DeleteRequest("types", "*"))
         }
         , RequestOptions.DEFAULT)
      if (deleteResult.hasFailures()) {
         log().warn("Deleting search index failed after ${deleteResult.took.millis}ms: ${deleteResult.buildFailureMessage()}")
      } else {
         log().info("Search index destroyed successfully - ${deleteResult.took.millis}ms")
      }

   }
}

enum class SearchEntryType {
   TYPE,
   ATTRIBUTE,
   POLICY,
   SERVICE,
   OPERATION
}

data class SearchEntry(val id: String, val name: String, val qualifiedName: String, val searchEntryType: SearchEntryType, val typeDoc: String?, val declaringEntityQualifiedName: String? = null)
