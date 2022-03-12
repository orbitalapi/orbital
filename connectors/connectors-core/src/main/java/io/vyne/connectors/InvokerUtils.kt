package io.vyne.connectors

import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.QualifiedName

fun TaxiQlQuery.resultType(): QualifiedName {
   return when {
      this.projectedType != null -> {
         this.projectedType!!.anonymousTypeDefinition?.toQualifiedName()
            ?: this.projectedType!!.concreteType?.toQualifiedName()
            ?: error("Projected type should contain either an anonymous type or a concrete type")
      }
      else -> {
         if (this.typesToFind.size > 1) {
            error("Multiple query types are not yet supported")
         } else {
            this.typesToFind.first().type
         }
      }
   }
}

fun List<MutableMap<String, Any>>.convertToTypedInstances(
   schema: Schema,
   datasource: DataSource,
   resultTypeName: QualifiedName,
   dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<TypedInstance> {
   val resultTaxiType = collectionTypeOrType(schema.taxi.type(resultTypeName))
   val typedInstances = this
      .map { columnMap ->
         TypedInstance.from(
            schema.type(resultTaxiType),
            columnMap,
            schema,
            source = datasource,
            evaluateAccessors = false
         )
      }
   return typedInstances.asFlow().flowOn(dispatcher)
}
