package io.vyne.connectors


object ConnectorUtils {
   private fun findMissingParameters(
      parameters: List<ConnectionDriverParam>,
      inputs: Map<ConnectionParameterName, Any?>
   ): List<ConnectionDriverParam> {
      return parameters.filter { it.required }
         .filter { !inputs.containsKey(it.templateParamName) || (inputs.containsKey(it.templateParamName) && inputs[it.templateParamName] == null) }
   }
   fun assertAllParametersPresent(
      parameters: List<ConnectionDriverParam>,
      inputs: Map<ConnectionParameterName, Any?>
   ): Map<ConnectionParameterName, Any> {
      val missing = findMissingParameters(parameters, inputs)
      val missingWithoutDefault = missing.filter { it.defaultValue == null }
      if (missingWithoutDefault.isNotEmpty()) {
         throw MissingConnectionParametersException(missingWithoutDefault)
      }
      return (inputs.filter { it.value != null } as Map<ConnectionParameterName, Any>) + missing.map { it.templateParamName to it.defaultValue!! }
   }
}
fun Array<out IConnectionParameter>.connectionParams(): List<ConnectionDriverParam> = this.map { it.param }

class MissingConnectionParametersException(private val parameters: List<ConnectionDriverParam>) :
   RuntimeException("The following parameters were not provided: ${parameters.joinToString { it.displayName }}")
