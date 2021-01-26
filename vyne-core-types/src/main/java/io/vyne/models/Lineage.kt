package io.vyne.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.query.RemoteCall

@JsonDeserialize(using = DataSourceDeserializer::class)
interface DataSource {
   @get:JsonProperty("dataSourceName")
   val name: String
}

interface DataSourceIncludedView

/**
 * Use this when a source has multiple independent sources, and callers
 * should traverse further to find the underlying sources.
 * It is not appropriate to use MixedSources in scalar types, which can
 * have only originated in a single source
 */
object MixedSources : DataSource {
   override val name: String = "Multiple sources"
}

object UndefinedSource : DataSource {
   override val name: String = "Undefined source"
}

data class FailedEvaluation(val message: String) : DataSource {
   override val name: String = "Failed evaluation"
}

/**
 * Indicates that the value was defined in a schema - ie., in Taxi, or
 * in the contract of a service
 */
object DefinedInSchema : DataSource {
   override val name: String = "Defined in schema"
}

data class OperationResult(val remoteCall: RemoteCall, val inputs: List<OperationParam>) : DataSource {
   data class OperationParam(val parameterName: String, val value: Any?)

   override val name: String = "Operation result"
}


sealed class MappedValue(val mappingType: MappingType) : DataSource {
   abstract val source: TypedInstance
   override val name = "Mapped"

   enum class MappingType {
      SYNONYM
   }
}

data class MappedSynonym(override val source: TypedInstance) : MappedValue(MappingType.SYNONYM)

/**
 * Indicates that the data was provided - typically as an input to a query.
 * Generally, this should only be used for top-most values
 */
object Provided : DataSource {
   override val name: String = "Provided"
}

object Calculated : DataSource {
   override val name: String
      get() = "Calculated"

}
