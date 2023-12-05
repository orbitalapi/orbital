package com.orbitalhq.cockpit.core.security.authorisation

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Extracts roles from within a JWT token by specifying a path
 */
@ConditionalOnProperty(
   name = ["vyne.security.open-idp.jwt-type"],
   havingValue = PathRolesExtractor.PathJwtKind,
   matchIfMissing = true
)
open class PathRolesExtractor(
   @Value("\${vyne.security.open-idp.jwt-roles-path}") val path: String
) : JwtRolesExtractor {

   companion object {
      const val PathJwtKind = "path"
   }

   override fun getRoles(jwt: Jwt): Set<String> {
      val parts = path.split(".")

      val consumedPath = mutableListOf<String>()

      val claimsAtPath = parts.foldIndexed(jwt.claims as Any) { index, acc, pathPart ->
         require(acc is Map<*,*>) { "Internal error: Expected a Map<*,*> at $pathPart, but got ${acc::class.simpleName}"}

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
            if (valueAtCurrentPath is Map<*,*>) {
               valueAtCurrentPath
            } else {
               error("$errorPrefix Expected to find a Map, but found a ${valueAtCurrentPath::class.simpleName}")
            }
         }

      } as List<String>
      return claimsAtPath.toSet()
   }
}
