package com.orbitalhq.cockpit.core.security.authorisation

import mu.KotlinLogging
import org.flywaydb.core.internal.util.AsciiTable
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

/**
 * A debugging tool which outputs the state of
 * API endpoints, documenting which roles are required to access them,
 * or if they are open.
 *
 * Enable by starting with --vyne.authorization.audit=true
 */
@Component
@ConditionalOnProperty("vyne.authorization.audit", havingValue = "true", matchIfMissing = false)
class AuthorizationAudit : ApplicationListener<ContextRefreshedEvent> {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun onApplicationEvent(event: ContextRefreshedEvent) {
      auditAuthOnEndpoints(event.applicationContext)
   }

   fun auditAuthOnEndpoints(context: ApplicationContext) {
      val controllers = context.getBeansWithAnnotation(RestController::class.java)
      val endpointsWithAuth = controllers.entries.flatMap { (beanName, controller) ->
         val controllerClass = controller::class.java

         controllerClass.declaredMethods
            .mapNotNull { method -> methodIsApiEndpoint(method) }
            .map { (method, requestMappings) ->
               val preAuthorizeAnnotations =
                  AnnotatedElementUtils.findAllMergedAnnotations(method, PreAuthorize::class.java)
               val springExpression: String? = when (preAuthorizeAnnotations.size) {
                   0 -> null
                   1 -> preAuthorizeAnnotations.single().value
                   else -> {
                      logger.error { "Method ${controllerClass.name} ${method.name} has multiple PreAuthorize annotations - this is a mistake" }
                      null
                   }
               }
               EndpointAuthDescription(
                  controllerClass, method, requestMappings, springExpression
               )
            }
      }
      logAsTable(endpointsWithAuth)

   }

   private fun logAsTable(endpointsWithAuth: List<EndpointAuthDescription>) {
      val sortedEndpoints = endpointsWithAuth.sortedBy { it.role ?: "## Open - no role required" }
      val columns = listOf("Class", "Method", "Endpoints", "Role")
      val rows = sortedEndpoints.map { endpoint ->
         listOf(
            endpoint.controller.name,
            endpoint.method.name,
            endpoint.endpoints.joinToString(",") {
               val methods = it.method.joinToString(",") { it.name }
               val paths = it.path.joinToString(",")
               "$methods : $paths"
            },
            endpoint.role ?: "## Open - no role required"
         )
      }
      val table = AsciiTable(
         columns,
         rows,
         true,
         "null",
         "Empty"
      ).render()
      logger.info { "Endpoint auth audit result: \n$table" }
   }

   private fun methodIsApiEndpoint(method: Method): Pair<Method, Set<RequestMapping>>? {
      val requestMappings = AnnotatedElementUtils.findAllMergedAnnotations(method, RequestMapping::class.java)
      return if (requestMappings.isEmpty()) {
         null
      } else {
         method to requestMappings
      }
   }
}

private data class EndpointAuthDescription(
   val controller: Class<*>,
   val method: Method,
   val endpoints: Set<RequestMapping>,
   val spel: String?
) {
   val role: String? = spel?.removeSurrounding(prefix = "hasAuthority('", suffix = "')")
}
