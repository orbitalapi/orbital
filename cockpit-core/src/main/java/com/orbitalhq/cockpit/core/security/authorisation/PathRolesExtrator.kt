package com.orbitalhq.cockpit.core.security.authorisation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component


/**
 * Extracts roles from within a JWT token by specifying a path
 */
@ConditionalOnProperty(
   name = ["vyne.security.open-idp.roles.format"],
   havingValue = SimplePathBasedRolesExtractor.PathJwtKind,
   matchIfMissing = false
)
@Component
class SimplePathBasedRolesExtractor(path: String) : BasePathRolesExtractor(path) {
   companion object {
      const val PathJwtKind = "path"
   }
   @Autowired
   constructor(idpConfig: VyneOpenIdpConnectConfig) : this(idpConfig.roles.path!!)
}


abstract class BasePathRolesExtractor(
   val path: String
) : JwtRolesExtractor {

   override fun getRoles(jwt: Jwt): Set<String> {
      val parts = path.split(".")

      val consumedPath = mutableListOf<String>()

      val claimsAtPath = parts.foldIndexed(jwt.claims as Any) { index, acc, pathPart ->
         require(acc is Map<*, *>) { "Internal error: Expected a Map<*,*> at $pathPart, but got ${acc::class.simpleName}" }

         consumedPath.add(pathPart)
         val errorPrefix = "Misconfigured roles path at index $index (${consumedPath.joinToString(".")})."
         if (!acc.containsKey(pathPart)) {
            error("$errorPrefix Expected to find a key $pathPart present, but it was not found. Present keys are ${acc.keys}")
         }
         val valueAtCurrentPath = acc.get(pathPart) ?: error("$errorPrefix Value was null.")
         val isLastPart = index == (parts.size - 1)

         if (isLastPart) {
            if (valueAtCurrentPath is List<*>) {
               valueAtCurrentPath
            } else {
               error("$errorPrefix Expected to find a list of strings containing roles, but found a ${valueAtCurrentPath::class.simpleName}")
            }
         } else {
            if (valueAtCurrentPath is Map<*, *>) {
               valueAtCurrentPath
            } else {
               error("$errorPrefix Expected to find a Map, but found a ${valueAtCurrentPath::class.simpleName}")
            }
         }

      } as List<String>
      return claimsAtPath.toSet()
   }
}
