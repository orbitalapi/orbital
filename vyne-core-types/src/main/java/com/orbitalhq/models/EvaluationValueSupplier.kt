package com.orbitalhq.models

import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Type
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.Argument
import lang.taxi.types.FormatsAndZoneOffset

/**
 * When evaluating expressions, a thing that can provide values.
 * Generally a TypedObjectFactory
 */
interface EvaluationValueSupplier {
   fun getValue(
       typeName: QualifiedName,
       queryIfNotFound: Boolean = false,
       allowAccessorEvaluation: Boolean = true
   ): TypedInstance

   fun getScopedFact(scope: Argument): TypedInstance
   fun getValue(attributeName: AttributeName): TypedInstance
   fun readAccessor(type: Type, accessor: Accessor, format: FormatsAndZoneOffset?): TypedInstance
   fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean, format: FormatsAndZoneOffset?): TypedInstance
}
