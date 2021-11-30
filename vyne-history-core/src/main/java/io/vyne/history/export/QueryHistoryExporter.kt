package io.vyne.history.export

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.utils.ExceptionProvider
import io.vyne.query.history.QuerySummary
import io.vyne.query.toCsv
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.models.TypeNamedInstance
import io.vyne.models.format.EmptyTypedInstanceInfo
import io.vyne.models.format.FirstTypedInstanceInfo
import io.vyne.models.format.FormatDetector
import io.vyne.models.format.ModelFormatSpec
import io.vyne.query.PersistedAnonymousType
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Schema
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

/**
 * Makes query results downloadable as CSV or JSON.
 *
 * As much as possible, we try to keep this streaming, and non-blocking
 *
 */
@FlowPreview
@Component
class QueryHistoryExporter(
   injectedMapper: ObjectMapper,
   private val resultRepository: QueryResultRowRepository,
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val schemaProvider: SchemaProvider,
   private val exceptionProvider: ExceptionProvider,
   modelFormatSpecs: List<ModelFormatSpec>
) {
   private val formatDetector = FormatDetector(modelFormatSpecs)
   private val objectMapper = injectedMapper
      .copy()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
   val vyneSetType = object : TypeReference<Set<PersistedAnonymousType>>() {}
   fun export(queryId: String, exportFormat: ExportFormat): Flow<CharSequence> {
      val querySummary = assertQueryIdIsValid(queryId)
      val results = querySummary.map {
          if (it.anonymousTypesJson == null) emptySet() else objectMapper.readValue(it.anonymousTypesJson!!, vyneSetType)
      }.flatMapMany { anonymousTypes ->
         resultRepository
            .findAllByQueryId(queryId)
            .map { Pair(it.asTypeNamedInstance(objectMapper), anonymousTypes) }
            .toFlux()
      }.asFlow()

      return when (exportFormat) {
         ExportFormat.CSV -> toCsv(results, schemaProvider.schema())
         ExportFormat.JSON ->
            // When we're exporting as JSON, we first wrap as an array, then wrap
            // the individual TypeNamedInstances
            Flux.concat(
               Flux.fromIterable(listOf("[")),
               results.withIndex().map { (index, typeNamedInstance) ->
                  // prepend commands in between the items.
                  val prefix = if (index > 0) {
                     ","
                  } else ""
                  prefix + objectMapper.writeValueAsString(typeNamedInstance.first.convertToRaw())
               }.asFlux(),
               Flux.fromIterable(listOf("]"))
            ).asFlow()
         ExportFormat.CUSTOM -> toCustomFormat(results)

      }
   }

   private fun toCustomFormat(results: Flow<Pair<TypeNamedInstance, Set<PersistedAnonymousType>>>): Flow<CharSequence> {
      val schema = schemaProvider.schema()
      return results
         .withIndex()
         .flatMapConcat { indexedValue ->
            val output = toModelFormattedString(schema, indexedValue.index, indexedValue.value)
            output?.let { flowOf(it) } ?: flowOf("")
         }
   }

   private fun toModelFormattedString(
      schema: Schema,
      index: Int,
      typedNamedInstancePersistedAnonymousTypePair: Pair<TypeNamedInstance, Set<PersistedAnonymousType>>): String? {
      val typeNamedInstance = typedNamedInstancePersistedAnonymousTypePair.first
      val anonymousTypeDefinitions = typedNamedInstancePersistedAnonymousTypePair.second
      val includeHeaders = index == 0
      return if (anonymousTypeDefinitions.isEmpty()) {
         val responseType = schema.type(typeNamedInstance.typeName)
         this.formatDetector.getFormatType(responseType)?.let { (metadata, spec) ->
            spec.serializer.write(
               typeNamedInstance,
               responseType,
               metadata,
               if (includeHeaders) FirstTypedInstanceInfo else EmptyTypedInstanceInfo)?.toString()
         }
      } else {
         anonymousTypeDefinitions.firstOrNull { it.name.fullyQualifiedName ==  typeNamedInstance.typeName}
            ?.let { persistedAnonymousType ->
               this.formatDetector.getFormatType(persistedAnonymousType.metadata)?.let { (metadata, spec) ->
                  spec.serializer.write(
                     typeNamedInstance,
                     persistedAnonymousType.attributes.keys,
                     metadata,
                     if (includeHeaders) FirstTypedInstanceInfo else EmptyTypedInstanceInfo
                  )?.toString()

               }
            }
      }
   }

   private fun assertQueryIdIsValid(queryId: String): Mono<QuerySummary> {
      return try {
         val querySummary = queryHistoryRecordRepository.findByQueryId(queryId)
         Mono.just(querySummary)
      } catch (exception: EmptyResultDataAccessException) {
         Mono.defer {
            throw exceptionProvider.notFoundException("No query with id $queryId was found")
         }
      }
   }
}

enum class ExportFormat {
   JSON, CSV, CUSTOM
}



