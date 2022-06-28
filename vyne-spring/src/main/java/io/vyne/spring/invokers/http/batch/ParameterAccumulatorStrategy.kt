package io.vyne.spring.invokers.http.batch

import io.vyne.models.*
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

/**
 * Accumulates inputs to an array.
 * eg:
 * Input1, Input2, Input3 -> [Input1, Input2, Input3]
 */
class AccumulateAsArray(private val parameter: Parameter) : ParameterAccumulatorStrategy {
   override fun build(inputs: List<Pair<Parameter, TypedInstance>>): List<Pair<Parameter, TypedInstance>> {
      val distinctParameters = inputs.map { it.first }.distinct()
      require(distinctParameters.size == 1) { "When accumulating batch of parameters to construct request, expected all the values would be for the same parameter within the operation, but found ${distinctParameters.joinToString { it.toString() }}" }
      return listOf(
         parameter to collectToTypedArray(inputs)
      )
   }
}

/**
 * Accumulates inputs as an array property on a request object.
 *
 * eg:
 * Input1, Input2, Input3 ->  {
 *   someParam: [Input1, Input2, Input3]
 * }
 */
class AccumulateAsArrayAttributeOnRequest(private val parameter: Parameter, private val schema: Schema) :
   ParameterAccumulatorStrategy {
   override fun build(inputs: List<Pair<Parameter, TypedInstance>>): List<Pair<Parameter, TypedInstance>> {
      val distinctParameters = inputs.map { it.first }.distinct()
      require(distinctParameters.size == 1) { "When accumulating batch of parameters to construct request, expected all the values would be for the same parameter within the operation, but found ${distinctParameters.joinToString { it.toString() }}" }

      val collectionOfTypedInstances = collectToTypedArray(inputs)
      val factBag = FactBag.of(listOf(collectionOfTypedInstances), schema)
      val requestObject = TypedObjectFactory(
         parameter.type,
         factBag,
         schema,
         source = MixedSources.singleSourceOrMixedSources(collectionOfTypedInstances)
      )
         .build()
      return listOf(parameter to requestObject)
   }
}
