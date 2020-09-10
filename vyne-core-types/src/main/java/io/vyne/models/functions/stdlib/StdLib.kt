package io.vyne.models.functions.stdlib

import io.vyne.VersionedSource
import io.vyne.models.functions.SelfDescribingFunction

object StdLib {
   val functions = listOf(
      Strings.functions
   ).flatten()

   val taxi = VersionedSource("vyne.stdlib", "0.0", functions
      .filterIsInstance<SelfDescribingFunction>()
      .namespacedTaxi())
}


fun List<SelfDescribingFunction>.namespacedTaxi():String {
   val result = groupBy { it.functionName.namespace }
      .map { (namespace,functions) ->
         val functionTaxi = functions.joinToString("\n") { it.taxiDeclaration }
         """namespace $namespace {
            |$functionTaxi
            |}
         """.trimMargin()
      }.joinToString("\n")
   return result

}

fun String.inNamespace(namespace:String):String {
   return """namespace $namespace {
      |$this
      |}
   """.trimMargin()
}
