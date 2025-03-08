package com.orbitalhq.metrics

import io.micrometer.core.instrument.Tag

enum class MetricTags(private val tagName:String) {
   ConnectionName("connectionName"),
   Topic("topic"),
   Queue("queue"),
   Operation("operation"),
   TableName("tableName"),
   StreamingJobState("jobState"),
   KafkaPartition("kafkaPartition"),
   KafkaGroupId("kafkaGroupId");


   fun of(value: Any):Tag {
      return Tag.of(this.tagName, value.toString())
   }
}
