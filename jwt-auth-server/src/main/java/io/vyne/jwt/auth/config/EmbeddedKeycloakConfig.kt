package io.vyne.jwt.auth.config

import io.vyne.jwt.auth.keycloack.EmbeddedKeycloakApplication
import io.vyne.jwt.auth.keycloack.EmbeddedKeycloakRequestFilter
import io.vyne.jwt.auth.keycloack.SimplePlatformProvider
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters
import org.keycloak.platform.Platform
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.naming.*
import javax.naming.spi.InitialContextFactory
import javax.naming.spi.NamingManager
import javax.sql.DataSource


@Configuration
class EmbeddedKeycloakConfig {
   @Bean
   @Throws(Exception::class)
   fun keycloakJaxRsApplication(
      keycloakServerProperties: KeycloakServerProperties,
      dataSource: DataSource,
      executorService: ExecutorService
   ): ServletRegistrationBean<HttpServlet30Dispatcher>? {
      mockJndiEnvironment(dataSource, executorService)
      EmbeddedKeycloakApplication.keycloakServerProperties = keycloakServerProperties
      val servlet = ServletRegistrationBean(HttpServlet30Dispatcher())
      servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication::class.java.getName())
      servlet.addInitParameter(
         ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
         keycloakServerProperties.contextPath
      )
      servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true")
      servlet.addUrlMappings(keycloakServerProperties.contextPath + "/*")
      servlet.setLoadOnStartup(1)
      servlet.isAsyncSupported = true
      return servlet
   }

   @Bean
   fun keycloakSessionManagement(keycloakServerProperties: KeycloakServerProperties): FilterRegistrationBean<EmbeddedKeycloakRequestFilter>? {
      val filter: FilterRegistrationBean<EmbeddedKeycloakRequestFilter> =
         FilterRegistrationBean<EmbeddedKeycloakRequestFilter>()
      filter.setName("Keycloak Session Management")
      filter.filter = EmbeddedKeycloakRequestFilter()
      filter.addUrlPatterns(keycloakServerProperties.contextPath + "/*")
      return filter
   }

   @Bean("fixedThreadPool")
   fun fixedThreadPool(): ExecutorService {
      return Executors.newFixedThreadPool(5)
   }

   @Bean
   @ConditionalOnMissingBean(name = ["springBootPlatform"])
   protected fun springBootPlatform(): SimplePlatformProvider {
      return Platform.getPlatform() as SimplePlatformProvider
   }

   @Throws(NamingException::class)
   private fun mockJndiEnvironment(dataSource: DataSource, threadPool: ExecutorService) {
      NamingManager.setInitialContextFactoryBuilder { env: Hashtable<*, *>? ->
         InitialContextFactory {
            object : InitialContext() {
               override fun lookup(name: Name): Any? {
                  return lookup(name.toString())
               }

               override fun lookup(name: String): Any? {
                  if ("spring/datasource" == name) {
                     return dataSource
                  }
                  if (name.startsWith("java:jboss/ee/concurrency/executor/")) {
                     return threadPool
                  }
                  return null
               }

               override fun getNameParser(name: String): NameParser {
                  return NameParser { n: String? -> CompositeName(n) }
               }

               override fun close() {
                  // NOOP
               }
            }
         }
      }
   }
}
