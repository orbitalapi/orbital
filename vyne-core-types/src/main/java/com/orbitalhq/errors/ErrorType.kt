package com.orbitalhq.errors

import com.orbitalhq.VersionedSource
import com.orbitalhq.VyneTypes
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.FailedSearch
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.EmptyTypeCache
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.types.PrimitiveType

// See design doc:
// https://projects.notional.uk/youtrack/articles/ORB-A-27/Error-Handling
object ErrorType {
   fun errorMessage(message: String, schema: Schema, source: DataSource = FailedSearch(message)): TypedInstance {
      return TypedInstance.from(
         type = schema.type(ErrorMessageQualifiedName),
         value = message,
         source = source,
         schema = schema
      )
   }

   val ErrorMessageQualifiedName = "${VyneTypes.NAMESPACE}.errors.ErrorMessage".fqn()

   val ErrorTypeDefinition = """namespace ${ErrorMessageQualifiedName.namespace} {
         |   type ${ErrorMessageQualifiedName.name} inherits String
         |
         |   // Empty base type
         |   model Error {}
         |
         |   model NotAuthorizedError inherits Error {
         |     message: ErrorMessage
         |   }
         |}""".trimMargin()

   val queryErrorVersionedSource = VersionedSource(
      "VyneQueryError",
      "0.1.0",
      ErrorTypeDefinition
   )

   val type = Type(
      ErrorMessageQualifiedName.fullyQualifiedName,
      sources = listOf(queryErrorVersionedSource),
      taxiType = PrimitiveType.STRING,
      typeCache = EmptyTypeCache,
      inheritsFromTypeNames = emptyList()
   )
}

