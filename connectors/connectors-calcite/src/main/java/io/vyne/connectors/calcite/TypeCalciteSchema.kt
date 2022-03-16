package io.vyne.connectors.calcite

import io.vyne.models.TypedInstance
import io.vyne.schemas.Type
import lang.taxi.types.ArrayType
import org.apache.calcite.schema.Table
import org.apache.calcite.schema.impl.AbstractSchema
import java.util.stream.Stream

class SingleVyneTypeCalciteSchema(
   private val type: Type,
   private val dataSource: Stream<TypedInstance>,
   private val schema: io.vyne.schemas.Schema): AbstractSchema() {
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
