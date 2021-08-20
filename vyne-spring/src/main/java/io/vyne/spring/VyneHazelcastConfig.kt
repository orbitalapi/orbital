package io.vyne.spring

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VyneHazelcastConfig {

   @Bean("hazelcast")
   @ConditionalOnProperty(VYNE_SCHEMA_PUBLICATION_METHOD, havingValue = "DISTRIBUTED")
   fun defaultHazelCastInstance(): HazelcastInstance {
      return Hazelcast.newHazelcastInstance()
   }

}
