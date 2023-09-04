package com.orbitalhq.connectors

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import lang.taxi.TaxiDocument
import lang.taxi.query.DiscoveryType
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.ArrayType
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type

fun TaxiQlQuery.resultType(): QualifiedName {
   return when {
      this.projectedType != null -> this.projectedType!!.toQualifiedName()
      this.typesToFind.size == 1 -> this.typesToFind.single().typeName
      this.typesToFind.size > 1 ->   error("Multiple query types are not yet supported")
      else -> error("Unexpected code branch determining result type")
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


fun collectionTypeOrType(type: Type): Type {
   return if (type is ArrayType) {
      type.parameters[0]
   } else {
      type
   }
}

fun getTypesToFind(query: TaxiQlQuery, taxiSchema: TaxiDocument): List<Pair<ObjectType, DiscoveryType>> {
    val typesToFind = query.typesToFind
        .map { discoveryType ->
            val collectionType = collectionTypeOrType(taxiSchema.type(discoveryType.typeName)) as ObjectType
            collectionType to discoveryType
        }
    return typesToFind
}
