package io.vyne.pipelines.jet.source.kafka

import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG
import org.junit.Test

class KafkaSourceBuilderTest {
   @Test
   fun `app config overrides pipeline defaults`() {
      val mergedConfig = KafkaPipelineConfig(
         props = mapOf(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "none",
         )
      ).mergeWithDefaults()
      mergedConfig[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG].should.equal("none")
      mergedConfig[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG].should.equal(KafkaPipelineConfig.DEFAULT_PROPS[ENABLE_AUTO_COMMIT_CONFIG])
   }

   @Test
   fun `pipeline spec values override app config and pipeline defaults`() {
      val appDefaults = KafkaPipelineConfig(
         props = mapOf(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "none",
         )
      )
      val mergedConfig = KafkaUtils.mergeConfig(
         KafkaTransportInputSpec(
            "topic",
            VersionedTypeReference.parse("Person"),
            mapOf(
               ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
            )
         ),
         appDefaults
      )
      mergedConfig[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG].should.equal("earliest")
   }
}
