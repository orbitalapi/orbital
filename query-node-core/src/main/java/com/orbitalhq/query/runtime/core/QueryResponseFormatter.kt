package com.orbitalhq.query.runtime.core

import com.google.common.net.MediaType
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QueryResultSerializer
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.SearchFailedException
import com.orbitalhq.query.runtime.FailedSearchResponse
import com.orbitalhq.query.runtime.core.csv.toCsv
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

@Component
class QueryResponseFormatter(modelFormatSpecs: List<ModelFormatSpec>) {
   private val formatDetector = FormatDetector(modelFormatSpecs)
   @FlowPreview
   fun convertToSerializedContent(
      queryResponse: QueryResponse,
      resultMode: ResultMode,
      requestedContentType: String,
      queryOptions: QueryOptions
   ): Pair<String,Flow<Any>> {
      return when (queryResponse) {
         is QueryResult -> this.convertToSerializedContentInternal(queryResponse, resultMode, requestedContentType, queryOptions)
         is FailedSearchResponse -> this.serialiseFailedResponse(queryResponse, requestedContentType)
         else -> error("Received unknown type of QueryResponse: ${this::class.simpleName}")
      }
   }

   private fun serialiseFailedResponse(
      failedSearchResponse: FailedSearchResponse,
      contentType: String
   ) : Pair<String,Flow<Any>> {
      return when (contentType) {
         TEXT_CSV -> contentType to flowOf(failedSearchResponse.message)
         // Assume everything else is JSON.  Return the entity, and let
         // Spring / Jackson take care of the serialization.
         else -> "application/json" to flowOf(failedSearchResponse)
      }
   }


   @FlowPreview
   private fun serialise(results: Flow<TypedInstance>, serializer: QueryResultSerializer, schema: Schema): Flow<Any> {
      return results
         .catch { error ->
            when (error) {
               is SearchFailedException -> {
                  throw ResponseStatusException(
                     HttpStatus.BAD_REQUEST,
                     error.message ?: "Search failed without a message"
                  )
               }
               else -> throw error
            }
         }
         .flatMapMerge { typedInstance ->
            // This is a smell.
            // I've noticed that when projecting, in this point of the code
            // we get individual typed instances.
            // However, if we're not projecting, we get a single
            // typed collection.
            // This meas that the shape of the response (array vs single)
            // varies based on the query, which is incorrect.
            // Therefore, unwrap collections here.
            // This smells, because it could be indicative of a problem
            // higher in the stack.
            if (typedInstance is TypedCollection) {
               typedInstance.map { serializer.serialize(it, schema) }
            } else {
               listOf(serializer.serialize(typedInstance, schema))
            }.filterNotNull()
               .asFlow()

         }
         .filterNotNull()
   }

   fun buildStreamingSerializer(resultMode: ResultMode, queryResponse: QueryResult, contentType: String, queryOptions: QueryOptions): QueryResultSerializer {
      logger.info { "Building streaming serializer for Query Response Type ${queryResponse.responseType} " +
         "with Accept header value $contentType and result mode $this" }
      return tryGetModelFormatSerialiser(resultMode, queryResponse) ?: buildSerializer(resultMode, queryResponse, contentType, queryOptions)
   }

   private fun buildSerializer(resultMode: ResultMode, queryResponse: QueryResult, requestedContentType: String, queryOptions: QueryOptions): QueryResultSerializer {
      logger.info {
         "Building serializer for Query Response Type ${queryResponse.responseType} " +
            "with ContentSerializationFormat header value $requestedContentType and result mode $resultMode"
      }
      return when (resultMode) {
         ResultMode.RAW -> RawResultsSerializer(queryOptions)
         ResultMode.SIMPLE, ResultMode.TYPED -> FirstEntryMetadataResultSerializer.forQueryResult(queryResponse, queryOptions)
         ResultMode.VERBOSE -> SerializedTypedInstanceSerializer(requestedContentType)
      }
   }


   private fun tryGetModelFormatSerialiser(resultMode: ResultMode, queryResult: QueryResult): QueryResultSerializer? {
      return if (resultMode == ResultMode.RAW && queryResult.responseType != null) {
         // Check whether the result type has a model format spec.
         val responseType =queryResult.responseType.collectionType ?: queryResult.responseType
         val modelSerializer = responseType?.let {
            this.formatDetector.getFormatType(responseType)?.let { (metadata, spec) ->
               ModelFormatSpecSerializer(spec, metadata)
            }
         }
         modelSerializer
      } else null
   }


   private fun getNonModelFormatSerialiser(
      requestedContentType: String,
      queryResult: QueryResult,
      resultMode: ResultMode,
      queryOptions: QueryOptions
      ): QueryResultSerializer {
      return if (requestedContentType == TEXT_CSV)
         buildSerializer(ResultMode.RAW, queryResult, requestedContentType, queryOptions)
      else
         buildSerializer(
            resultMode,
            queryResult,
            requestedContentType,
            queryOptions
         )
   }

   @FlowPreview
   private fun convertToSerializedContentInternal(
      queryResult: QueryResult,
      resultMode: ResultMode,
      requestedContentType: String,
      queryOptions: QueryOptions
   ): Pair<String,Flow<Any>> {

      val modelFormattedResult  =  tryGetModelFormatSerialiser(resultMode, queryResult)?.let {serializer ->
         serializer.contentType to serialise(queryResult.results, serializer, queryResult.schema)
      }

      return if (modelFormattedResult != null) {
         modelFormattedResult
      } else {
         val serializer = getNonModelFormatSerialiser(requestedContentType, queryResult, resultMode, queryOptions)
         when (requestedContentType) {
            TEXT_CSV -> MediaType.CSV_UTF_8.toString() to toCsv(queryResult.results, serializer, queryResult.schema)
            // Default everything else to JSON
            else -> serializer.contentType to serialise(queryResult.results, serializer, queryResult.schema)
         }
      }
   }
}


