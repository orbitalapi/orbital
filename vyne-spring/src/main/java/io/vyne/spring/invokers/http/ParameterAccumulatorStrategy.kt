package io.vyne.spring.invokers.http

import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.schemas.Parameter
import io.vyne.schemas.Schema

private fun collectToTypedArray(inputs: List<Pair<Parameter, TypedInstance>>): TypedCollection {
   val collectionValues = inputs.map { it.second }
   return TypedCollection.from(collectionValues)
}

sealed interface ParameterAccumulatorStrategy {
   /**
    * Takes all the inputs gathered each member in the current batch,
    * and returns them as a set of parameters for the batch operation.
    */
   fun build(inputs: List<Pair<Parameter, TypedInstance>>): List<Pair<Parameter, TypedInstance>>
}

class AccumulateAsArray(private val parameter: Parameter) : ParameterAccumulatorStrategy {
   override fun build(inputs: List<Pair<Parameter, TypedInstance>>): List<Pair<Parameter, TypedInstance>> {
      val distinctParameters = inputs.map { it.first }.distinct()
      require(distinctParameters.size == 1) { "When accumulating batch of parameters to construct request, expected all the values would be for the same parameter within the operation, but found ${distinctParameters.joinToString { it.toString() }}" }
      return listOf(
         parameter to collectToTypedArray(inputs)
      )
   }
}

class AccumulateAsArrayAttributeOnRequest(private val parameter: Parameter, private val schema: Schema) :
   ParameterAccumulatorStrategy {
   override fun build(inputs: List<Pair<Parameter, TypedInstance>>): List<Pair<Parameter, TypedInstance>> {
      val distinctParameters = inputs.map { it.first }.distinct()
      require(distinctParameters.size == 1) { "When accumulating batch of parameters to construct request, expected all the values would be for the same parameter within the operation, but found ${distinctParameters.joinToString { it.toString() }}" }

      val requestObject = TypedInstance.from(parameter.type, collectToTypedArray(inputs), schema)
      return listOf(parameter to requestObject)
   }

}
