package io.vyne.connectors.kafka.builders

import io.vyne.connectors.kafka.*

class KafkaConnectionBuilder  {
   enum class Parameters(override val param: KafkaConnectionParam) : IKafkaConnectionParamEnum {
      BROKERS(KafkaConnectionParam("brokers", SimpleDataType.STRING)),
      TOPIC(KafkaConnectionParam("topic", SimpleDataType.STRING, defaultValue = "5432")),
      OFFSET(KafkaConnectionParam("offset", SimpleDataType.STRING, defaultValue = "latest")),
   }

   val parameters: List<KafkaConnectionParam> = Parameters.values().connectionParams()

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
