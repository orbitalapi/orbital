package io.vyne.queryService.query

import io.vyne.models.*
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.ResultMode

// Purpose of this class is to create results list with references dataSources
// List of dataSources is an array with no duplicates, individual result reference dataSources via index
class QueryResultGraph(
   private val results: Map<QuerySpecTypeNode, TypedInstance?>,
   private val resultMode: ResultMode) {
   private val dataSourceIndexMap: MutableMap<DataSource, Int> = mutableMapOf()
   val resultSources: MutableList<DataSource> = mutableListOf()
   fun buildResultsVerbose(): Map<String, List<TypeNamedInstance>> {
      if (resultMode == ResultMode.SIMPLE) {
         return emptyMap()
      }

      return results.map { (key, value) ->
         key.type.name.parameterizedName to mapToTypeNamedInstances(value)
      }.toMap()
   }

   private fun mapToTypeNamedInstances(value: TypedInstance?): List<TypeNamedInstance> {
      return when (value) {
         is TypedCollection -> {
            return value.value.map { mapToTypeNamedInstance(it) }
         }
         is TypedInstance -> {
            val sourceReference = calculateSourceReference(value)
            listOf(TypeNamedInstance(value.type.fullyQualifiedName, value.value))
         }
         else -> TODO("Value type ${value?.javaClass} not supported")
      }
   }

   private fun mapToTypeNamedInstance(value: TypedInstance?): TypeNamedInstance {
      return when (value) {
         is TypedCollection -> {
            val sourceReference = calculateSourceReference(value)
            val values = value.value.map { mapToTypeNamedInstance(it) }
            TypeNamedInstance(value.type.fullyQualifiedName, values)
         }
         is TypedObject -> {
            val sourceReference = calculateSourceReference(value)
            val values = value.value.map { it.key to mapToTypeNamedInstance(it.value) }.toMap()
            TypeNamedInstance(value.type.fullyQualifiedName, values)
         }
         is TypedInstance -> {
            val sourceReference = calculateSourceReference(value)
            TypeNamedInstance(value.type.fullyQualifiedName, value.value)
         }
         else -> TODO("Value type ${value?.javaClass} not supported")
      }
   }

   private fun calculateSourceReference(value: TypedInstance): Int? {
      if (resultMode == ResultMode.SIMPLE) {
         return null
      }
      return value.source?.let { source ->
         dataSourceIndexMap.computeIfAbsent(source) { dataSource ->
            val theSource = when(dataSource) {
               is OperationResult -> toOperationResultWithTypeNamedInstanceParams(dataSource)
               else -> dataSource
            }
            val sourceIndex = resultSources.size
            resultSources.add(theSource)
            sourceIndex
         }
      }
   }

   private fun toOperationResultWithTypeNamedInstanceParams(dataSource: OperationResult): OperationResult {
      val inputs = dataSource.inputs.map {
         val paramName = it.parameterName
         val paramValue = it.value
         when (paramValue) {
            is TypedInstance -> OperationResult.OperationParam(paramName, mapToTypeNamedInstance(paramValue))
            else -> it
         }
      }
      return OperationResult(dataSource.remoteCall, inputs)
   }
}
