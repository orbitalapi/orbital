package com.orbitalhq.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.tracing.TracingEventIdSet
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.orElse
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

@JsonDeserialize(using = DataSourceDeserializer::class)
interface DataSource {
   @get:JsonProperty("dataSourceName")
   val name: String
   val id: String

   val failedAttempts: List<DataSource>

   fun appendFailedAttempts(failedAttempts: List<DataSource>): DataSource {
      logger.error { "Attempt of unsupported operation, updating failedAttempts of datasource ${this::class.simpleName}" }
      return this
   }
}

interface DatasourceWithInputs : DataSource {
   val inputs: List<TypedInstance>
}

object DataSourceUpdater {
   fun update(typedInstance: TypedInstance, newDataSource: DataSource): TypedInstance {
      return when (typedInstance) {
         is TypedValue -> typedInstance.copy(source = newDataSource)
         is TypedObject -> typedInstance.copy(source = newDataSource)
         is TypedCollection -> typedInstance.copy(source = newDataSource)
         is TypedEnumValue -> typedInstance.copy(source = newDataSource)
         is TypedNull -> typedInstance.copy(source = newDataSource)
         else -> error("Unhandled type of TypedInstance: ${typedInstance::class.simpleName}")
      }
   }


}

/**
 * DataSource that should not be persisted - the values are always the same
 */
enum class StaticDataSources(val dataSource: StaticDataSource) {
   PROVIDED(Provided),
   DEFINED_IN_SCHEMA(DefinedInSchema),
   UNDEFINED(UndefinedSource),
   MIXED_SOURCES(MixedSources);

   companion object {
      private val byId = StaticDataSources.values().associateBy { it.dataSource.id }
      fun isStatic(id: String) = this.byId.containsKey(id)
      fun forId(id: String): StaticDataSource = byId[id]?.dataSource ?: error("$id is not a known static data source")
   }
}

interface StaticDataSource : DataSource {
   override val id: String
      get() = this.name

   override val failedAttempts: List<DataSource>
      get() = emptyList()
}

interface DataSourceIncludedView

/**
 * Use this when a source has multiple independent sources, and callers
 * should traverse further to find the underlying sources.
 * It is not appropriate to use MixedSources in scalar types, which can
 * have only originated in a single source
 */
object MixedSources : StaticDataSource {
   override val name: String = "Multiple sources"
   fun singleSourceOrMixedSources(instances: Collection<TypedInstance>): DataSource {
      return instances.map { it.source }.distinct().singleOrNull() ?: MixedSources
   }
}

object UndefinedSource : StaticDataSource {
   override val name: String = "Undefined source"
}

data class FailedEvaluation(
   override val message: String, override val id: String = UUID.randomUUID().toString(),
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource, DataSourceWithErrorMessage {
   override val name: String = "Failed evaluation"
}

/**
 * Indicates that the value was defined in a schema - ie., in Taxi, or
 * in the contract of a service
 */
object DefinedInSchema : StaticDataSource {
   override val name: String = "Defined in schema"
}

data class FailedParsingSource(
   val value: Any,
   val error: String,
   override val id: String = UUID.randomUUID().toString(),
   override val failedAttempts: List<DataSource> = emptyList(),
   override val name: String = "Failed parsing"
) : DataSource, DataSourceWithErrorMessage {
   override val message: String = error
}

/**
 * A lightweight version of OperationResult.
 * Really just has the id's required to load the real operation result,
 * plus a handful of things to help with debugging.
 *
 * Crucially, does not contain the actual result.
 */
data class OperationResultReference(
   val remoteCallId: String,
   val remoteCallResponseId: String,
   val wasSuccessful: Boolean,
   val inputs: List<OperationResult.OperationParam>,
   val operationName: QualifiedName,
   override val sourceEventId: TracingEventIdSet? = null,
   override val failedAttempts: List<DataSource> = emptyList(),
   override val id: String = remoteCallId
) : DataSource, DataSourceWithLinkedTraceEvent {
   companion object {
      const val NAME: String = "Operation result"
   }

   val operationDisplayName: String = operationName.shortDisplayName
   val serviceDisplayName: String = OperationNames.serviceName(operationName).fqn().shortDisplayName

   override val name: String = NAME



   override fun appendFailedAttempts(failedAttempts: List<DataSource>): DataSource {
      return copy(failedAttempts = this.failedAttempts + failedAttempts)
   }
}


/**
 * Wraps an OperationResult.
 *
 * Generally, usage of OperationResult as a datasource
 * is discouraged, as they can contain a very large
 * amount of data, which leads to heap and storage problems.
 *
 * Generally, a OperationResultReference is preferred.
 *
 * However, sometimes (in the case of streaming results),
 * it's not possible to construct an OperationResultReference
 * within invokers.
 *
 * In that case, use a OperationResultDataSourceWrapper as an interim measure,
 * and convert to OperationResultReference at earliest possible point.
 *
 */
data class OperationResultDataSourceWrapper(
   val operationResult: OperationResult,
   /**
    * The TraceEvent ID that contains the result event that triggered this
    */
   override val sourceEventId: TracingEventIdSet? = null
) : DataSource, DataSourceWithLinkedTraceEvent {
   val operationResultReferenceSource = operationResult.asOperationReferenceDataSource(sourceEventId)
   override val name: String
      get() = operationResultReferenceSource.name
   override val id: String
      get() = operationResultReferenceSource.id
   override val failedAttempts: List<DataSource> = operationResultReferenceSource.failedAttempts
}

/**
 * Indicates that the data source exposes a tracing event Id.
 * This will be linked to a child span, if the TypedInstance that this data source is associated with
 * spawns a new projection / mutation span
 */
interface DataSourceWithLinkedTraceEvent {
   // Nullable because this concept is being gradually introduced.
   // But you should definitely populate it.
   val sourceEventId: TracingEventIdSet?
}

data class OperationResult(
   val remoteCall: RemoteCall,
   val inputs: List<OperationParam>,
   val failedAttempts: List<DataSource> = emptyList()
) /*: DataSource */ {
   companion object {
      const val NAME: String = "Operation result"
      fun from(
         parameters: List<Pair<Parameter, TypedInstance>>,
         remoteCall: RemoteCall
      ): OperationResult {
         return OperationResult(remoteCall, buildParameters(parameters))
      }

      fun buildParameters(parameters: List<Pair<Parameter, TypedInstance>>): List<OperationParam> {
         return parameters.map { (param, instance) ->
            OperationParam(param.name.orElse("Unnamed"), instance.toTypeNamedInstance())
         }
      }

      fun fromTypedInstances(
         parameters: List<TypedInstance>,
         remoteCall: RemoteCall
      ): OperationResult {
         return OperationResult(remoteCall, parameters.map { instance ->
            OperationParam(
               parameterName = "Unnamed",
               value = instance.toTypeNamedInstance()
            )
         })
      }
   }

   data class OperationParam(val parameterName: String, val value: Any?)

   val name: String = NAME
   val id: String = remoteCall.remoteCallId

//   fun appendFailedAttempts(failedAttempts: List<DataSource>): DataSource {
//      return OperationResult(
//         remoteCall, inputs,
//         failedAttempts = this.failedAttempts + failedAttempts
//      )
//   }

   fun asOperationReferenceDataSource(
      /**
       * The trace event that contains the result of the operation.
       * Optional, because this is being introduced. But you should definitely populate this
       */
      traceEventId: TracingEventIdSet? = null
   ): OperationResultReference {
      return OperationResultReference(
         remoteCallId = this.remoteCall.remoteCallId,
         remoteCallResponseId = this.remoteCall.responseId,
         wasSuccessful = !remoteCall.isFailed,
         inputs = inputs,
         operationName = remoteCall.operationQualifiedName,
         sourceEventId = traceEventId
      )
   }

}


sealed class MappedValue(val mappingType: MappingType, override val id: String = UUID.randomUUID().toString()) :
   DataSource {
   abstract val source: TypedInstance
   override val name = "Mapped"
   override val failedAttempts: List<DataSource> = emptyList()

   enum class MappingType {
      SYNONYM
   }
}

// Implementation note:  This used to pass TypedNamedInstance for the source, rather than TypedInstance
// However, this can lead to inconsistent lineage, as we only capture the id of the parent dataSource, not the
// dataSource itself.  That means if no other types reference the dataSource, it's not caputred, and we end up
// with orpahned nodes.
data class MappedSynonym(override val source: TypedInstance) :
   MappedValue(MappingType.SYNONYM) {
   override val id: String = "From ${source.typeName}::${source.value.orElse("Null")}"
}

/**
 * Indicates that the data was provided - typically as an input to a query.
 * Generally, this should only be used for top-most values
 */
object Provided : StaticDataSource {
   override val name: String = "Provided"
}


@Deprecated("Use EvaluatedExpression instead")
object Calculated : DataSource {
   override val name: String = "Calculated"
   override val id: String = name
   override val failedAttempts: List<DataSource> = emptyList()
}

data class EvaluatedExpression(
   val expressionTaxi: String,
   override val inputs: List<TypedInstance>,
   override val id: String = UUID.randomUUID().toString()
) : DataSource, DatasourceWithInputs {
   override val name: String = "Evaluated expression"
   override val failedAttempts: List<DataSource> = emptyList()
}

data class FailedEvaluatedExpression(
   val expressionTaxi: String,
   override val inputs: List<TypedInstance>,
   val errorMessage: String,
   val unresolvedInputs: List<QualifiedName> = emptyList(),
   val inputInError: TypedInstance? = null,
   val cause: DataSource? = null,
   override val id: String = UUID.randomUUID().toString(),
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource, DatasourceWithInputs, DataSourceWithErrorMessage {
   override val name: String = "Failed evaluated expression"
   override fun toString(): String {

      return "FailedEvaluatedExpression: Expression $expressionTaxi failed with message $errorMessage"
   }

   override val message: String = errorMessage

   /**
    * Returns the unresolvedInputs provided to this expression,
    * plus any reslated to the provided inputs (which in turn could
    * have a failedExpression)
    */
   val allUnresolvedInputs: List<QualifiedName>
      get() {
         return this.inputs.filterIsInstance<TypedNull>()
            .flatMap {
               val source = it.source
               if (source is FailedEvaluatedExpression) {
                  source.allUnresolvedInputs
               } else {
                  emptyList()
               }
            } + this.unresolvedInputs
      }
}

data class ValueLookupReturnedNull(
   override val message: String,
   val requestedTypeName: QualifiedName,
   override val id: String = Ids.fastUuid(),
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource, DataSourceWithErrorMessage {
   override val name: String = "Failed lookup"
}

data class AmbiguousResult(
   val message: String,
   val requestedTypeName: QualifiedName,
   override val id: String = Ids.fastUuid(),
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource {
   override val name: String = "Lookup result was ambiguous"

   companion object {
      fun isAmbiguousResult(value: TypedNull): Boolean {
         return value.source is AmbiguousResult
      }
   }
}

data class FailedSearch(val message: String, override val failedAttempts: List<DataSource> = emptyList()) : DataSource {
   override val name: String = "FailedSearch"
   override val id: String = Ids.fastUuid()

    companion object {
       fun isFailedSearch(value: TypedInstance): Boolean {
          return value is TypedNull && value.source is FailedSearch
       }
    }
}

interface DataSourceWithErrorMessage {
   val message: String
}
