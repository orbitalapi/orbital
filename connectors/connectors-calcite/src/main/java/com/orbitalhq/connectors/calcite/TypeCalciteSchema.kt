package com.orbitalhq.connectors.calcite

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Type
import lang.taxi.types.ArrayType
import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractSchema
import java.util.stream.Stream

class SingleVyneTypeCalciteSchema(
   private val type: Type,
   private val dataSource: Stream<TypedInstance>,
   private val schema: com.orbitalhq.schemas.Schema): AbstractSchema() {
   override fun getTableMap(): MutableMap<String, Table> {
      val parametrizedTypeName = type.collectionTypeName ?: type.qualifiedName
      val tableName =parametrizedTypeName.shortDisplayName.toUpperCase()
      val vyneTypeTable = TypeTable(schema.type(parametrizedTypeName), dataSource, schema)
      return mutableMapOf(tableName to vyneTypeTable)
   }

   override fun getTypeNames(): MutableSet<String> {
      return this.tableMap.keys
   }
}
