package com.orbitalhq.connectors.kafka

import org.junit.Before
import org.junit.Test

class KafkaWriterTest  : BaseKafkaContainerTest() {

   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }

   @Test
   fun `can write a query result to kafka`() {

   }

}
