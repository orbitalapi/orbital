package io.vyne.queryService.query

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.format.FormatDetector
import io.vyne.models.format.ModelFormatSpec
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.QueryResultSerializer
import io.vyne.query.ResultMode
import io.vyne.query.SearchFailedException
import io.vyne.queryService.csv.toCsv
import io.vyne.schema.api.SchemaSourceProvider
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
class QueryResponseFormatter(modelFormatSpecs: List<ModelFormatSpec>, private val schemaProvider: SchemaSourceProvider) {
   private val formatDetector = FormatDetector(modelFormatSpecs)
   @FlowPreview
   fun convertToSerializedContent(
      queryResponse: QueryResponse,
      resultMode: ResultMode,
      contentType: String
   ): Flow<Any> {
      return when (queryResponse) {
         is QueryResult -> this.convertToSerializedContentInternal(queryResponse, resultMode, contentType)
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
   private fun serialise(results: Flow<TypedInstance>, serializer: QueryResultSerializer): Flow<Any> {
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
               typedInstance.map { serializer.serialize(it) }
            } else {
               listOf(serializer.serialize(typedInstance))
            }.filterNotNull()
               .asFlow()

         }
         .filterNotNull()
   }

   fun buildStreamingSerializer(resultMode: ResultMode, queryResponse: QueryResult, contentType: String?): QueryResultSerializer {
      logger.info { "Building streaming serializer for Query Response Type ${queryResponse.responseType} " +
         "with Accept header value $contentType and result mode $this" }
      return tryGetModelFormatSerialiser(resultMode, queryResponse) ?: buildSerializer(resultMode, queryResponse, contentType)
   }

   private fun buildSerializer(resultMode: ResultMode, queryResponse: QueryResult, contentType: String?): QueryResultSerializer {
      logger.info { "Building serializer for Query Response Type ${queryResponse.responseType} " +
         "with Accept header value $contentType and result mode $this" }
      return when (resultMode) {
         ResultMode.RAW -> RawResultsSerializer
         ResultMode.SIMPLE, ResultMode.TYPED -> FirstEntryMetadataResultSerializer.forQueryResult(queryResponse)
         ResultMode.VERBOSE -> TypeNamedInstanceSerializer
      }
   }


   private fun tryGetModelFormatSerialiser(resultMode: ResultMode, queryResult: QueryResult): QueryResultSerializer? {
      return if (resultMode == ResultMode.RAW && queryResult.responseType != null) {
         // Check whether the result type has a model format spec.
         val currentSchema = schemaProvider.schema()
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
      resultMode: ResultMode): QueryResultSerializer {
      return if (contentType == TEXT_CSV)
         buildSerializer(ResultMode.RAW, queryResult, contentType)
      else
         buildSerializer(
            resultMode,
            queryResult,
            contentType
         )
   }

   @FlowPreview
   private fun convertToSerializedContentInternal(
      queryResult: QueryResult,
      resultMode: ResultMode,
      contentType: String
   ): Flow<Any> {

      val modelFormattedResult  =  tryGetModelFormatSerialiser(resultMode, queryResult)?.let {
         serialise(queryResult.results, it)
      }

      return if (modelFormattedResult != null) {
         modelFormattedResult
      } else {
         val serializer = getNonModelFormatSerialiser(contentType, queryResult, resultMode)
         when (contentType) {
            TEXT_CSV -> toCsv(queryResult.results, serializer)
            // Default everything else to JSON
            else -> serialise(queryResult.results, serializer)
         }
      }
   }
}


