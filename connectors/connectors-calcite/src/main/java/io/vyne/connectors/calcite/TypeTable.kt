package io.vyne.connectors.calcite

import io.vyne.models.TypedInstance
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.TypeAlias
import mu.KotlinLogging
import org.apache.calcite.DataContext
import org.apache.calcite.linq4j.AbstractEnumerable
import org.apache.calcite.linq4j.Enumerable
import org.apache.calcite.linq4j.Enumerator
import org.apache.calcite.rel.type.RelDataType
import org.apache.calcite.rel.type.RelDataTypeFactory
import org.apache.calcite.schema.ScannableTable
import org.apache.calcite.schema.impl.AbstractTable
import java.util.stream.Stream

private val logger = KotlinLogging.logger {  }
class TypeTable(private val type: Type, private val dataSource: Stream<TypedInstance>, private val schema: Schema): AbstractTable(), ScannableTable {
   override fun getRowType(relDataTypeFactory: RelDataTypeFactory): RelDataType {
      val structure =  type.attributes.map { (fieldName, field) ->
         val relDataType =
         relDataTypeFactory.createJavaType( CalciteMapping.getJavaType(schema.type(field.type).taxiType))
         fieldName to relDataType
      }.toMap().entries.toList()
     return  relDataTypeFactory.createStructType(structure)
   }

   override fun scan(dataContex: DataContext): Enumerable<Array<Any?>> {
      return TypedInstanceStreamEnumerable(dataSource)
   }
}

class  TypedInstanceStreamEnumerable(private val dataSource: Stream<TypedInstance>): AbstractEnumerable<Array<Any?>>() {
   override fun enumerator(): Enumerator<Array<Any?>> {
      return TypedInstanceStreamEnumerator(dataSource)
   }
}

class TypedInstanceStreamEnumerator(private val dataSource: Stream<TypedInstance>): Enumerator<Array<Any?>> {
   private val streamIterator = dataSource.iterator()
   private var current: Array<Any?>? = null
   override fun close() {
      dataSource.close()
   }

   override fun current(): Array<Any?>? {
     return current
   }

   override fun moveNext(): Boolean {
      return if (streamIterator.hasNext()) {
         val typedObjectAttrMap = streamIterator.next().value as Map<String, TypedInstance>
         this.current = typedObjectAttrMap.values.map { it.value }.toTypedArray()
         true
      } else false
   }

   override fun reset() {
      logger.info { "reset" }

   }
}

object CalciteMapping {
   fun isTaxiTypeApplicableForSimpleSqlMapping(type: lang.taxi.types.Type): Boolean {
      return when {
         type is PrimitiveType -> true
         type is ObjectType && type.fields.isEmpty()  && type.basePrimitive != null-> true
         else -> false
      }
   }

   fun getJavaType(type: lang.taxi.types.Type): Class<*> {
      return when {
         type is PrimitiveType -> PrimitiveTypes.getJavaType(type)
         type is ObjectType && type.fields.isEmpty() -> {
            require(type.basePrimitive != null) { "Type ${type.qualifiedName} is scalar, but does not have a primitive type.  This is unexpected" }
            getJavaType(type.basePrimitive!!)
         }
         type is TypeAlias  -> {
            require(type.basePrimitive != null) { "Type ${type.qualifiedName} is scalar, but does not have a primitive type.  This is unexpected" }
            getJavaType(type.basePrimitive!!)
         }
         else -> error("Add support for Taxi type ${type::class.simpleName}")
      }
   }
}
