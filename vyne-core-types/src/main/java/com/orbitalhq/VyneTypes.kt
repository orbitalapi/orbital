package com.orbitalhq

import com.orbitalhq.schemas.fqn

object VyneTypes {
   /**
    * Defined as 'var' rather than 'val' or 'const' is to have the ability to customise the root namespace for 'built-in' Orbital Types.
    * Though not ideal, this is the easiest / quickest way to achieve it.
    */
   var NAMESPACE = "com.orbitalhq"
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
