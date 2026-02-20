package com.orbitalhq.models

import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.FactDiscoveryStrategy
import com.orbitalhq.models.facts.FactSearch
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.facts.SearchableDataContext
import com.orbitalhq.schemas.*
import lang.taxi.accessors.Accessor
import lang.taxi.accessors.Argument
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.types.FormatsAndZoneOffset

/**
 * Turns a FactBag into a ValueSupplier for evaluating expressions.
 * Lightweight way to provide access to TypedInstances in expression evaluation that's happening
 * outside of object construction (eg., when evaluating an expression inside a function)
 *
 * Supports model scan strategies in FactBag, so asking for a type will return the closest match
 * considering polymorphism
 */
class FactBagValueSupplier(
   private val facts: FactBag,
   private val schema: Schema,
   /**
    * The value supplier to use when evaluating statements scoped with "this"
    * If none passed, an empty value bag is used, so evaluations will fail.
    *
    * Generally, this should be a TypedObjectFactory.
    */
   private val scopedValueProvider: ScopedValueProvider = FactBagScopeValueProvider(facts, schema),
   private val typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES,

   ) : EvaluationValueSupplier {
   companion object {
      fun of(
         facts: List<TypedInstance>,
         schema: Schema,
         thisScopeValueSupplier: ScopedValueProvider = FactBagScopeValueProvider.empty(schema),
         typeMatchingStrategy: TypeMatchingStrategy = TypeMatchingStrategy.ALLOW_INHERITED_TYPES
      ): EvaluationValueSupplier {
         return FactBagValueSupplier(FactBag.of(facts, schema), schema, thisScopeValueSupplier, typeMatchingStrategy)
      }

      fun empty(schema: Schema): EvaluationValueSupplier {
         return of(emptyList(), schema)
      }
   }

   override val hasDataContext: Boolean = true
   override val dataContext: SearchableDataContext = facts
   override suspend fun withAdditionalScopedFacts(scopedFacts: List<ScopedFact>): FactBagValueSupplier {

      return FactBagValueSupplier(
         this.facts,
         schema,
         this.scopedValueProvider.withAdditionalScopedFacts(scopedFacts),
         typeMatchingStrategy
      )
   }

   override val inPlaceQueryEngine: InPlaceQueryEngine?
      get() {
         return when (scopedValueProvider) {
            is TypedObjectFactory -> scopedValueProvider.inPlaceQueryEngine
            else -> null
         }
      }

   override suspend fun getValue(
       typeName: QualifiedName,
       queryIfNotFound: Boolean,
       allowAccessorEvaluation: Boolean,
       constraints: List<Constraint>
   ): TypedInstance {
      val type = schema.type(typeName)
      val fact = facts.getFactOrNull(
         FactSearch.findType(
            type,
            strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE,
            matcher = typeMatchingStrategy
         )
      )
//      val fact = facts.getFactOrNull(type, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
      return fact ?: TypedNull.create(
         type,
         FailedSearch("Type ${typeName.shortDisplayName} was not found in the provided set of facts")
      )
   }

   override suspend fun getValue(attributeName: AttributeName): TypedInstance {
      return scopedValueProvider.getValue(attributeName)
   }

   override fun getScopedFact(scope: Argument): TypedInstance {
      return scopedValueProvider.getScopedFact(scope)
   }

   override fun getScopedFactOrNull(scope: Argument): TypedInstance? {
      return scopedValueProvider.getScopedFactOrNull(scope)
   }

   override suspend fun readAccessor(type: Type, accessor: Accessor, format: FormatsAndZoneOffset?): TypedInstance {
      // This method shouldn't be called.
      error("readAccessor is not supported by this class")
   }

   override suspend fun readAccessor(
      type: QualifiedName,
      accessor: Accessor,
      nullable: Boolean,
      format: FormatsAndZoneOffset?
   ): TypedInstance {
      // This method shouldn't be called.
      error("readAccessor is not supported by this class")
   }
}
