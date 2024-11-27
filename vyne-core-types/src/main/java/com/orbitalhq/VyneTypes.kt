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
   val UsernameTypeName = "${VyneTypes.NAMESPACE}.Username".fqn()
   val UsernameTypeDefinition = """namespace ${UsernameTypeName.namespace} {
         |   type ${UsernameTypeName.name} inherits String
         |}""".trimMargin()
}

object AuthClaimType {
   val AuthClaimsTypeName = "${VyneTypes.NAMESPACE}.auth.AuthClaims".fqn()
   val AuthClaimsTypeDefinition = """namespace ${AuthClaimsTypeName.namespace} {
         |   model ${AuthClaimsTypeName.name} {
         |
         |   }
         |}""".trimMargin()
}

object OmitNullsType {
   val namespace = "${VyneTypes.NAMESPACE}.models"
   val NAME = "$namespace.OmitNulls"

   val imports: String =  listOf(NAME).joinToString("\n") { "import $it" }

   val schema = """
   namespace  $namespace {
      annotation ${NAME.fqn().name} 
   }
""".trimMargin()
}
