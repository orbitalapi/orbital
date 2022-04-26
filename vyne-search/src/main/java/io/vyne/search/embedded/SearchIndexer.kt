package io.vyne.search.embedded

import com.google.common.base.Stopwatch
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.Field
import io.vyne.schemas.Operation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.lucene.document.Document
import org.apache.lucene.document.TextField
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.apache.lucene.document.Field as LuceneField

@Component
class IndexOnStartupTask(private val indexer: SearchIndexer, private val schemaStore: SchemaStore) {
   private val logger = KotlinLogging.logger {}
   init {
      logger.info("Initializing search, indexing current schema")
      try {
         indexer.createNewIndex(schemaStore.schemaSet)
      } catch (e: IllegalArgumentException) {
         // Thrown by lucene when an index has changed config
         // Lets trash the existing index, and retry
         logger.warn("Exception thrown when updating index.  ( ${e.message} ) - will attempt to recover by deleting existing index, and rebuilding")
         indexer.deleteAndRebuildIndex(schemaStore.schemaSet)
      } catch (e: CompilationException) {
         logger.warn("Compilation exception found when trying to create search indexes on startup - we'll just wait. \n ${e.message}")
      }

   }
}

@Component
class SearchIndexer(
   private val schemaStore: SchemaStore,
   private val searchIndexRepository: SearchIndexRepository,
   private val reindexThreadPool: ExecutorService = Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("VyneSearchIndexer-%d").build())): InitializingBean {
   private val logger = KotlinLogging.logger {}

   override fun afterPropertiesSet() {
      Flux.from(schemaStore.schemaChanged).subscribe { event ->
         reindexThreadPool.submit {
            logger.info("Schema set changed, re-indexing")
            deleteAndRebuildIndex(event.newSchemaSet)
         }
      }
   }

   internal fun deleteAndRebuildIndex(schemaSet: SchemaSet) {
      if (!hasValidSchema(schemaSet)) {
         logger.warn("Not deleting and rebuilding index, as there is no valid schema at present")
      }
      searchIndexRepository.destroyAndInitialize()
      createNewIndex(schemaSet)
   }

   private fun hasValidSchema(schemaSet: SchemaSet): Boolean {
      return try {
         schemaSet.schema
         true
      } catch (e:Exception) {
         logger.warn("Exception thrown when accessing schema, there's likely compilation errors: ${e.message}")
         false
      }
   }

   internal fun createNewIndex(schemaSet: SchemaSet) {
      val stopwatch = Stopwatch.createStarted()
      val schema = try {
         schemaSet.schema
      } catch (e:Exception) {
         logger.warn("Exception thrown when accessing schema - there's likely compilation errors. Aborting searchIndex creation")
         return
      }

      val searchEntries = schema.types.flatMap { searchIndexEntry(it) } +
         schema.operations.map { searchIndexEntry(it) } +
         schema.services.map { searchIndexEntry(it) } +
         schema.dynamicMetadata.map { searchIndexEntryForAnnotation(it) } +
         schema.metadataTypes.map { searchIndexEntryForAnnotation(it) }

      if (searchEntries.isEmpty()) {
         logger.warn("No members in the schema, so not creating search entries")
         return
      }

      val searchDocs = searchEntries.map { searchEntry ->
         Document().apply {

            add(TextField(SearchField.QUALIFIED_NAME.fieldName, searchEntry.qualifiedName, LuceneField.Store.YES))
            add(TextField(SearchField.NAME.fieldName, searchEntry.name, LuceneField.Store.YES))
            add(TextField(SearchField.MEMBER_TYPE.fieldName, searchEntry.searchEntryType.name, LuceneField.Store.YES))
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
      logger.info("Created search index with ${searchDocs.size} entries in $elapsed ms")
   }

   private fun searchIndexEntryForAnnotation(annotationQualifiedName: QualifiedName): SearchEntry {
      // TODO pass Metadata here so that we can index params as well.
      return SearchEntry(
         annotationQualifiedName.parameterizedName,
         annotationQualifiedName.name,
         annotationQualifiedName.parameterizedName,
         SearchEntryType.ANNOTATION,
         typeDoc = null)
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

   private fun searchIndexEntry(service: Service): SearchEntry {
      return SearchEntry(
         service.name.fullyQualifiedName,
         service.name.shortDisplayName,
         service.name.fullyQualifiedName,
         SearchEntryType.SERVICE,
         service.typeDoc,// TODO :  Support typeDOc on operations.
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
   QUALIFIED_NAME("qualifiedName", HighlightMethod.SUBSTRING, 1.5f),
   NAME("name", HighlightMethod.SUBSTRING, 3.0f),
   TYPEDOC("typeDoc", HighlightMethod.HIGHLIGHTER, 1f),
   FIELD_ON_TYPE("fieldNameOnType", HighlightMethod.HIGHLIGHTER, 1.2f),
   MEMBER_TYPE("memberType", HighlightMethod.HIGHLIGHTER, 0.01f),
   NAME_AS_WORDS("nameAsWords", HighlightMethod.HIGHLIGHTER, 0.7f);


   enum class HighlightMethod {
      SUBSTRING,
      HIGHLIGHTER,
      NONE;
   }
}

enum class SearchEntryType {
   TYPE,
   ATTRIBUTE,
   POLICY,
   SERVICE,
   OPERATION,
   ANNOTATION,
   UNKNOWN;

   companion object {
      fun fromName(name:String?):SearchEntryType {
         return if (name == null) {
            UNKNOWN
         } else {
            valueOf(name)
         }
      }
   }
}

data class SearchEntry(
   val id: String,
   val name: String,
   val qualifiedName: String,
   val searchEntryType: SearchEntryType,
   val typeDoc: String?,
   val fieldName: String? = null
)
