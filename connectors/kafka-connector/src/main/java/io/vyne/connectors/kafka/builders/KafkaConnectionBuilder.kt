package io.vyne.connectors.kafka.builders

import io.vyne.connectors.ConnectionDriverParam
import io.vyne.connectors.IConnectionParameter
import io.vyne.connectors.SimpleDataType
import io.vyne.connectors.connectionParams

object KafkaConnectionBuilder {
   enum class Parameters(override val param: ConnectionDriverParam) : IConnectionParameter {
      BROKERS(ConnectionDriverParam("brokers", SimpleDataType.STRING)),
      GROUP_ID(ConnectionDriverParam("groupId", SimpleDataType.STRING)),
//      TOPIC(ConnectionDriverParam("topic", SimpleDataType.STRING)),
//      OFFSET(
//         ConnectionDriverParam(
//            "offset",
//            SimpleDataType.STRING,
//            defaultValue = "latest",
//            allowedValues = listOf("earliest", "latest")
//         )
//      ),
   }

   val parameters: List<ConnectionDriverParam> = Parameters.values().connectionParams()
}

private fun <K, V> Map<K, V>.remove(keysToExclude: List<K>): Map<K, V> {
   return filterKeys { !keysToExclude.contains(it) };
}

fun String.substitute(inputs: Map<String, Any>): String {
   return inputs.entries.fold(this) { acc, entry ->
      val (key, value) = entry
      acc.replace("{$key}", value.toString())
   }
}
