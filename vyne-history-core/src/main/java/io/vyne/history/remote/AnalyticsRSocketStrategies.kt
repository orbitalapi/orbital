//package io.vyne.history.remote
//
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.http.codec.cbor.KotlinSerializationCborDecoder
//import org.springframework.http.codec.cbor.KotlinSerializationCborEncoder
//import org.springframework.messaging.rsocket.RSocketStrategies
//
//@Configuration
//class AnalyticsRSocketStrategies {
//
//   companion object {
//      const val ANALYTICS_RSOCKET_STRATEGIES = "ANALYTICS_RSOCKET_STRATEGIES"
//   }
//
//   @Bean(ANALYTICS_RSOCKET_STRATEGIES)
//   fun serde():RSocketStrategies {
//      return RSocketStrategies.builder()
//         .encoder(KotlinSerializationCborEncoder())
//         .decoder(KotlinSerializationCborDecoder())
//         .build()
//   }
//}
