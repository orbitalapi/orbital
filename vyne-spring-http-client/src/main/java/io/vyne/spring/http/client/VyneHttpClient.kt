package io.vyne.spring.http.client

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

class VyneHttpClient(val restTemplate: RestTemplate,  val vyneUrl: String) {
   inline fun <reified T : Any> vyneQuery(query: String): T {
      val (results, _) = submitVyneQuery<T>(query)
      return results
   }

    inline fun <reified T : Any> submitVyneQuery(query: String): Pair<T, HttpHeaders> {
      val result: ResponseEntity<T> = restTemplate.postForEntity<T>("$vyneUrl/api/vyneql", query)
      val body: T = result.body!!
      val headers: HttpHeaders = result.headers
      return body to headers
   }
}


