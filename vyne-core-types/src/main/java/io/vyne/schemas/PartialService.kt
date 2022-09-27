package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lang.taxi.Operator
import lang.taxi.services.FilterCapability
import lang.taxi.services.QueryOperationCapability
import lang.taxi.services.SimpleQueryCapability


@JsonDeserialize(`as` = DefaultPartialService::class)
@JsonIgnoreProperties(ignoreUnknown = true)
interface PartialService {
   val name: QualifiedName
   val operations: List<PartialOperation>
   val queryOperations: List<PartialQueryOperation>
   val tableOperations: List<PartialOperation>
   val streamOperations: List<PartialOperation>
   val metadata: List<Metadata>
   val typeDoc: String?
}

data class DefaultPartialService(
   override val name: QualifiedName,
   override val operations: List<PartialOperation>,
   override val queryOperations: List<PartialQueryOperation>,
   override val tableOperations: List<PartialOperation>,
   override val streamOperations: List<PartialOperation>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?,
) : PartialService

@JsonDeserialize(`as` = DefaultPartialOperation::class)
@JsonIgnoreProperties(ignoreUnknown = true)
interface PartialOperation {
   val qualifiedName: QualifiedName
   val parameters: List<PartialParameter>
   val metadata: List<Metadata>
   val typeDoc: String?
   val returnTypeName: QualifiedName
}

data class DefaultPartialOperation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<out PartialParameter>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?,
   override val returnTypeName: QualifiedName
) : PartialOperation

@JsonDeserialize(`as` = DefaultPartialParameter::class)
@JsonIgnoreProperties(ignoreUnknown = true)
interface PartialParameter {
   val name: String?
   val typeName: QualifiedName
   val metadata: List<Metadata>
}

data class DefaultPartialParameter(
   override val name: String?,
   override val typeName: QualifiedName,
   override val metadata: List<Metadata>
) : PartialParameter

@JsonDeserialize(`as` = DefaultPartialQueryOperation::class)
@JsonIgnoreProperties(ignoreUnknown = true)
interface PartialQueryOperation : PartialOperation {
   val grammar: String

   @get:JsonDeserialize(contentUsing = QueryOperationCapabilityDeserializer::class)
   val capabilities: List<QueryOperationCapability>
   val hasFilterCapability: Boolean
   val supportedFilterOperations: List<Operator>
}

data class DefaultPartialQueryOperation(
   override val qualifiedName: QualifiedName,
   override val parameters: List<out PartialParameter>,
   override val metadata: List<Metadata>,
   override val typeDoc: String?,
   override val returnTypeName: QualifiedName,
   override val grammar: String,
   override val capabilities: List<QueryOperationCapability>,
   override val hasFilterCapability: Boolean,
   override val supportedFilterOperations: List<Operator>
) : PartialQueryOperation


class QueryOperationCapabilityDeserializer : JsonDeserializer<QueryOperationCapability>() {
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): QueryOperationCapability {
      when (p.currentToken) {
         JsonToken.VALUE_STRING -> {
            val stringToken = p.valueAsString
            val isSimpleQueryCapability = SimpleQueryCapability.values().any { it -> it.name == stringToken }
            return if (isSimpleQueryCapability) {
               SimpleQueryCapability.valueOf(stringToken)
            } else {
               error(
                  "Unknown query capability: $stringToken - expected one of ${
                     SimpleQueryCapability.values().joinToString(",")
                  }"
               )
            }
         }

         JsonToken.START_OBJECT -> {
            val iterator = p.readValuesAs(FilterCapability::class.java)
            return iterator.next()
         }

         else -> error("Unhandled deserialization of QueryOperationCapability")
      }

   }

}
