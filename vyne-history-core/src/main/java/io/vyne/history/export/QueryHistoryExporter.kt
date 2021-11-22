package io.vyne.history.export

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.utils.ExceptionProvider
import io.vyne.query.history.QuerySummary
import io.vyne.query.toCsv
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.query.PersistedAnonymousType
import io.vyne.schemaStore.SchemaProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
   private val exceptionProvider: ExceptionProvider
) {
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
   JSON, CSV
}



