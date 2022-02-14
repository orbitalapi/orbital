package io.vyne.spring.http.client

import com.winterbe.expekt.should
import io.vyne.spring.http.client.config.VyneHttpClientConfig
import io.vyne.spring.http.client.config.VyneSpringHttpClientConfiguration
import org.junit.Test

class UnsecureVyneHttpClientTest {
   @Test
   fun `when client id is empty non secure Vyne Http Client is returned`() {
      val testClass = VyneSpringHttpClientConfiguration()
      val vyneHttpClient = testClass.vyneHttpClient(VyneHttpClientConfig(
         secret = "it is a secret",
         tokenUri = "http://my-token"))

      vyneHttpClient.vyneUrl.should.equal("http://localhost:9022")
      vyneHttpClient.restTemplate.interceptors.should.be.empty

   }

   @Test
   fun `when client secret is empty non secure Vyne Http Client is returned`() {
      val testClass = VyneSpringHttpClientConfiguration()
      val vyneHttpClient = testClass.vyneHttpClient(VyneHttpClientConfig(
         id = "vyne-api-client",
         tokenUri = "http://my-token"))

      vyneHttpClient.vyneUrl.should.equal("http://localhost:9022")
      vyneHttpClient.restTemplate.interceptors.should.be.empty
   }

   @Test
   fun `when tokenUri is empty non secure Vyne Http Client is returned`() {
      val testClass = VyneSpringHttpClientConfiguration()
      val vyneHttpClient = testClass.vyneHttpClient(
         VyneHttpClientConfig(
            id = "vyne-api-client",
            secret = "secret")
      )

      vyneHttpClient.vyneUrl.should.equal("http://localhost:9022")
      vyneHttpClient.restTemplate.interceptors.should.be.empty
   }
}
