package io.vyne.queryService

import lang.taxi.utils.log
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriBuilder
import java.lang.IllegalStateException
import javax.servlet.http.HttpServletRequest

/**
 * This class provides simple proxying to Cask
 */
@RestController
class CaskService(private val discoveryClient: DiscoveryClient, private val restTemplate: RestTemplate = RestTemplate()) {

   @PostMapping("/api/ingest/**")
   fun submitToCask(request: HttpServletRequest, @RequestBody payload: String): ResponseEntity<Any> {
      val requestUri = request.requestURI;
      val queryString = request.queryString
      val caskInstances = discoveryClient.getInstances("cask")
      if (caskInstances.isEmpty()) {
         throw IllegalStateException("No cask service is running")
      }
      val caskInstance = caskInstances.random()
      val caskServiceUri = caskInstance.uri

      log().info("Resolved cask to instance at $caskServiceUri")
      val caskRequestUri = "$caskServiceUri$requestUri?$queryString"

      val response = restTemplate.postForEntity<Any>(caskRequestUri, payload)
      log().info("Received response from cask : ${response.statusCodeValue} ${response.body}")
      return response
   }
}
