package com.orbitalhq

import com.orbitalhq.schemas.fqn

object VyneTypes {
   const val NAMESPACE = "com.orbitalhq"
}

object UserType {
   val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
   val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()
}

object JWTClaimType {
   val AuthClaims = "${VyneTypes.NAMESPACE}.auth.AuthClaims".fqn()
   val AuthClaimsTypeDefinition = """namespace ${AuthClaims.namespace} {
         |   model ${AuthClaims.name} {
         |
         |   }
         |}""".trimMargin()
}
