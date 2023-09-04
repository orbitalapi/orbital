package com.orbitalhq.query.runtime.core

import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneTypes
import com.orbitalhq.models.FailedSearch
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.EmptyTypeCache
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.PrimitiveType

object ErrorType {
   fun error(message: String, schema: Schema): TypedInstance {
      return TypedInstance.from(
         type = schema.type(ErrorTypeQualifiedName),
         value = message,
         source = FailedSearch(message),
         schema = schema
      )
   }

   private val ErrorTypeQualifiedName = "${VyneTypes.NAMESPACE}.Error".fqn()

   private val QUERY_ERROR_TYPEDEF = """namespace ${ErrorTypeQualifiedName.namespace} {
         |   type ${ErrorTypeQualifiedName.name} inherits String
         |}""".trimMargin()

   val queryErrorVersionedSource =  VersionedSource(
      "VyneQueryError",
      "0.1.0",
      QUERY_ERROR_TYPEDEF
   )

   val type = Type(
      ErrorTypeQualifiedName.fullyQualifiedName,
      sources = listOf(queryErrorVersionedSource),
      taxiType = PrimitiveType.STRING,
      typeCache = EmptyTypeCache,
      inheritsFromTypeNames = emptyList()
   )
}

