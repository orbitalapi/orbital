package io.vyne.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.query.RemoteCall


class DataSourceDeserializer : JsonDeserializer<DataSource>() {
   private val mapper = jacksonObjectMapper()
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DataSource {
      val rawMap = p.readValueAs(Any::class.java)
      return when(rawMap) {
         is Map<*, *> -> deserializeMap(rawMap as Map<Any, Any>)
         else -> UndefinedSource
      }
   }

   private fun deserializeMap(rawMap: Map<Any, Any>): DataSource {
      val isSimpleDataSource = rawMap.size == 1 && rawMap.containsKey("dataSourceName")
      if (isSimpleDataSource) {
         return when(val name = rawMap["dataSourceName"]) {
            Provided.name -> Provided
            MixedSources.name -> MixedSources
            DefinedInSchema.name -> DefinedInSchema
            UndefinedSource.name -> UndefinedSource
            else -> TODO("Unknown dataSourceName=${name}")
         }
      }
      val isMappedValueDataSource = rawMap.size == 2 && rawMap.containsKey("mappingType")
      if (isMappedValueDataSource) {
         return when(val mappingType = rawMap["mappingType"]) {
            MappedSynonym.mappingType.name -> MappedSynonym
            else -> TODO("Unknown mappingType=${mappingType}")
         }
      }
      val isRemoteCall = rawMap.containsKey("remoteCall")
      if (isRemoteCall) {
         val remoteCall = mapper.convertValue(rawMap["remoteCall"], RemoteCall::class.java)
         val inputs = (rawMap["inputs"] as List<Map<String, Any?>>).mapNotNull { mapToOperationParam(it) }
         return OperationResult(remoteCall, inputs)
      }
      return Provided
   }

   private fun mapToOperationParam(param: Map<String, Any?>): OperationResult.OperationParam? {
      if (param.size == 2 && param.containsKey("parameterName") && param.containsKey("value")) {
         val parameterName = param["parameterName"] as String
         val value = mapper.convertValue(param["value"], TypeNamedInstance::class.java)
         return OperationResult.OperationParam(parameterName, value)
      }
      return null
   }

}
