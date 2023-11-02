package com.orbitalhq.query.runtime.core

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.query.*
import com.orbitalhq.query.runtime.FailedSearchResponse
import com.orbitalhq.query.runtime.core.csv.toCsv
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

private val logger = KotlinLogging.logger {}

@Component
class QueryResponseFormatter(modelFormatSpecs: List<ModelFormatSpec>, private val schemaProvider: SchemaProvider) {
   private val formatDetector = FormatDetector(modelFormatSpecs)
   @FlowPreview
   fun convertToSerializedContent(
      queryResponse: QueryResponse,
      resultMode: ResultMode,
      contentType: String,
      queryOptions: QueryOptions
   ): Flow<Any> {
      return when (queryResponse) {
         is QueryResult -> this.convertToSerializedContentInternal(queryResponse, resultMode, contentType, queryOptions)
         is FailedSearchResponse -> this.serialiseFailedResponse(queryResponse, contentType)
         else -> error("Received unknown type of QueryResponse: ${this::class.simpleName}")
      }
   }

   private fun serialiseFailedResponse(
      failedSearchResponse: FailedSearchResponse,
      contentType: String
   ) : Flow<Any> {
      return when (contentType) {
         TEXT_CSV -> flowOf(failedSearchResponse.message)
         // Assume everything else is JSON.  Return the entity, and let
         // Spring / Jackson take care of the serialization.
         else -> flowOf(failedSearchResponse)
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

   fun buildStreamingSerializer(resultMode: ResultMode, queryResponse: QueryResult, contentType: String?, queryOptions: QueryOptions): QueryResultSerializer {
      logger.info { "Building streaming serializer for Query Response Type ${queryResponse.responseType} " +
         "with Accept header value $contentType and result mode $this" }
      return tryGetModelFormatSerialiser(resultMode, queryResponse) ?: buildSerializer(resultMode, queryResponse, contentType, queryOptions)
   }

   private fun buildSerializer(resultMode: ResultMode, queryResponse: QueryResult, contentType: String?, queryOptions: QueryOptions): QueryResultSerializer {
      logger.info {
         "Building serializer for Query Response Type ${queryResponse.responseType} " +
            "with ContentSerializationFormat header value $contentType and result mode $resultMode"
      }
      return when (resultMode) {
         ResultMode.RAW -> RawResultsSerializer(queryOptions)
         ResultMode.SIMPLE, ResultMode.TYPED -> FirstEntryMetadataResultSerializer.forQueryResult(queryResponse, queryOptions)
         ResultMode.VERBOSE -> SerializedTypedInstanceSerializer(contentType)
      }
   }


   private fun tryGetModelFormatSerialiser(resultMode: ResultMode, queryResult: QueryResult): QueryResultSerializer? {
      return if (resultMode == ResultMode.RAW && queryResult.responseType != null) {
         // Check whether the result type has a model format spec.
         val currentSchema = schemaProvider.schema
         val responseTypeFqnm = queryResult.responseType!!
         val responseType = when {
            currentSchema.hasType(responseTypeFqnm) -> currentSchema.type(responseTypeFqnm)
            queryResult.anonymousTypes.any { it.name.parameterizedName == queryResult.responseType } -> queryResult.anonymousTypes.first { it.name.parameterizedName == queryResult.responseType }
            else -> null
         }
         val modelSerializer = responseType?.let {
            this.formatDetector.getFormatType(responseType)?.let { (metadata, spec) ->
               ModelFormatSpecSerializer(spec, metadata)
            }
         }
         modelSerializer
      } else null
   }


   private fun getNonModelFormatSerialiser(
      contentType: String,
      queryResult: QueryResult,
      resultMode: ResultMode,
      queryOptions: QueryOptions
      ): QueryResultSerializer {
      return if (contentType == TEXT_CSV)
         buildSerializer(ResultMode.RAW, queryResult, contentType, queryOptions)
      else
         buildSerializer(
            resultMode,
            queryResult,
            contentType,
            queryOptions
         )
   }

   @FlowPreview
   private fun convertToSerializedContentInternal(
      queryResult: QueryResult,
      resultMode: ResultMode,
      contentType: String,
      queryOptions: QueryOptions
   ): Flow<Any> {

      val modelFormattedResult  =  tryGetModelFormatSerialiser(resultMode, queryResult)?.let {
         serialise(queryResult.results, it, queryResult.schema)
      }

      return if (modelFormattedResult != null) {
         modelFormattedResult
      } else {
         val serializer = getNonModelFormatSerialiser(contentType, queryResult, resultMode, queryOptions)
         when (contentType) {
            TEXT_CSV -> toCsv(queryResult.results, serializer, queryResult.schema)
            // Default everything else to JSON
            else -> serialise(queryResult.results, serializer, queryResult.schema)
         }
      }
   }
}


