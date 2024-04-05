package com.orbitalhq.models

import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.Argument
import lang.taxi.types.FormatsAndZoneOffset

/**
 * When evaluating expressions, a thing that can provide values.
 * Generally a TypedObjectFactory
 */
interface EvaluationValueSupplier : ScopedValueProvider {
   fun getValue(
       typeName: QualifiedName,
       queryIfNotFound: Boolean = false,
       allowAccessorEvaluation: Boolean = true
   ): TypedInstance

   fun readAccessor(type: Type, accessor: Accessor, format: FormatsAndZoneOffset?): TypedInstance
   fun readAccessor(type: QualifiedName, accessor: Accessor, nullable: Boolean, format: FormatsAndZoneOffset?): TypedInstance
}

interface ScopedValueProvider {
   fun getScopedFact(scope: Argument): TypedInstance
   fun getScopedFactOrNull(scope: Argument): TypedInstance?
   fun getValue(attributeName: AttributeName): TypedInstance
}

class FactBagScopeValueProvider(private val factBag: FactBag):ScopedValueProvider {
   override fun getScopedFact(scope: Argument): TypedInstance {
      return factBag.getScopedFact(scope).fact
   }

   override fun getScopedFactOrNull(scope: Argument): TypedInstance? {
      return factBag.getScopedFactOrNull(scope)?.fact
   }

   override fun getValue(attributeName: AttributeName): TypedInstance {
      TODO("Not yet implemented")
   }
   companion object {
      fun empty():FactBagScopeValueProvider = FactBagScopeValueProvider(FactBag.empty())
   }
}


