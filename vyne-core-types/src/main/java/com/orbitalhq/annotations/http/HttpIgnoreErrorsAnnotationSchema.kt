package com.orbitalhq.annotations.http

import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn

object HttpIgnoreErrorsAnnotationSchema {
    val namespace = "${VyneTypes.NAMESPACE}.http.operations"
    val NAME = "$namespace.HttpIgnoreErrors"

    val imports: String =  listOf(NAME).joinToString("\n") { "import $it" }

    val schema = """
namespace  $namespace {
   type HttpStatusCode inherits String

   annotation ${NAME.fqn().name} {
      responseCodes: HttpStatusCode[]
   }
}
"""
}