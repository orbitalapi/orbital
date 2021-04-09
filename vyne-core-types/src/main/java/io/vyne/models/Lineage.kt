package io.vyne.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.query.RemoteCall
import io.vyne.schemas.Parameter
import io.vyne.utils.orElse
import java.util.*

@JsonDeserialize(using = DataSourceDeserializer::class)
interface DataSource {
   @get:JsonProperty("dataSourceName")
   val name: String
   val id: String
}

/**
 * DataSource that should not be persisted - the values are always the same
 */
enum class StaticDataSources(val dataSource: StaticDataSource) {
   PROVIDED(Provided),
   DEFINED_IN_SCHEMA(DefinedInSchema),
   UNDEFINED(UndefinedSource),
   MIXED_SOURCES(MixedSources);

   private val byId = StaticDataSources.values().associateBy { it.dataSource.id }
   fun isStatic(id: String) = this.byId.containsKey(id)
   fun forId(id: String): StaticDataSource = byId[id]?.dataSource ?: error("$id is not a known static data source")
}

interface StaticDataSource : DataSource {
   override val id: String
      get() = this.name
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
}

object UndefinedSource : StaticDataSource {
   override val name: String = "Undefined source"
}

data class FailedEvaluation(val message: String, override val id: String = UUID.randomUUID().toString()) : DataSource {
   override val name: String = "Failed evaluation"
}

/**
 * Indicates that the value was defined in a schema - ie., in Taxi, or
 * in the contract of a service
 */
object DefinedInSchema : StaticDataSource {
   override val name: String = "Defined in schema"
}

data class OperationResult(val remoteCall: RemoteCall, val inputs: List<OperationParam>) : DataSource {
   companion object {
      const val NAME: String = "Operation result"
      fun from(parameters: List<Pair<Parameter, TypedInstance>>,
               remoteCall: RemoteCall):OperationResult {
         return OperationResult(remoteCall, parameters.map { (param, instance) ->
            OperationParam(param.name.orElse("Unnamed"), instance)
         })
      }
   }
   data class OperationParam(val parameterName: String, val value: Any?)

   override val name: String = NAME
   override val id: String = remoteCall.remoteCallId
}


sealed class MappedValue(val mappingType: MappingType, override val id: String = UUID.randomUUID().toString()) :
   DataSource {
   abstract val source: TypedInstance
   override val name = "Mapped"

   enum class MappingType {
      SYNONYM
   }
}

data class MappedSynonym(override val source: TypedInstance, override val id: String = UUID.randomUUID().toString()) :
   MappedValue(MappingType.SYNONYM)

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
}

data class EvaluatedExpression(
   val expressionTaxi: String,
   val inputs: List<TypedInstance>,
   override val id: String = UUID.randomUUID().toString()
) : DataSource {
   override val name: String = "Evaluated expression"
}

data class FailedEvaluatedExpression(
   val expressionTaxi: String,
   val inputs: List<TypedInstance>,
   val errorMessage: String,
   override val id: String = UUID.randomUUID().toString()
) : DataSource {
   override val name: String = "Failed evaluated expression"
}
