package io.vyne.spring.http.client

import com.winterbe.expekt.should
import io.vyne.spring.http.client.config.VyneHttpClientConfig
import io.vyne.spring.http.client.config.VyneSpringHttpClientConfiguration
import io.vyne.spring.http.client.secure.OAuthClientCredentialsRestTemplateInterceptor
import org.junit.Test

class SecureVyneHttpClientTest {
   @Test
   fun `when client id secret and token Uri is provided secure Vyne Http Client is returned`() {
      val testClass = VyneSpringHttpClientConfiguration()
      val vyneHttpClient = testClass.vyneHttpClient(VyneHttpClientConfig(
         id = "vyne-api-client", secret = "secret", tokenUri = "http://give-me-token"))

      vyneHttpClient.vyneUrl.should.equal("http://localhost:9022")
      vyneHttpClient.restTemplate.interceptors.first().should.instanceof(OAuthClientCredentialsRestTemplateInterceptor::class.java)
   }
}
