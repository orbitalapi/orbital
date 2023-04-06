//package io.vyne.query.runtime.http
//
//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import com.google.common.io.Resources
//import com.zaxxer.hikari.HikariConfig
//import io.kotest.core.spec.style.DescribeSpec
//import io.kotest.matchers.collections.shouldNotBeEmpty
//import io.micrometer.core.instrument.simple.SimpleMeterRegistry
//import io.vyne.query.runtime.QueryMessage
//import io.vyne.spring.config.VyneSpringCacheConfiguration
//import kotlinx.coroutines.flow.toList
//import org.springframework.web.reactive.function.client.WebClient
//
//class QueryConsumerTest : DescribeSpec({
//
//   it("should execute a query from only a json") {
//      val json = Resources.getResource("find-films-request.json").readText()
//      val objectMapper = jacksonObjectMapper()
//         .findAndRegisterModules()
//      val request = objectMapper
//         .readValue<QueryMessage>(json)
//
//
//      val vyneFactory = StandaloneVyneFactory(
//         HikariConfig(),
//         SimpleMeterRegistry(),
//         objectMapper,
//         WebClient.builder(),
//         VyneSpringCacheConfiguration()
//      )
//      val consumer = QueryConsumer(vyneFactory)
//
//      val resulFLow = consumer.executeQuery(request)
//      val results = resulFLow.toList()
//      results.shouldNotBeEmpty()
//      TODO()
//
//   }
//
//})
