package io.vyne.cask.query.vyneql

import arrow.core.getOrHandle
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.findActiveTables
import io.vyne.cask.query.CaskBadRequestException
import io.vyne.cask.query.CaskDAO
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.vyneql.DiscoveryType
import io.vyne.vyneql.VyneQLQueryString
import io.vyne.vyneql.VyneQlCompiler
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.Field
import lang.taxi.types.FieldReference
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import org.springframework.stereotype.Component

data class SqlStatement(val sql: String, val params: List<Any>)
typealias ColumnName = String
@Component
class VyneQlSqlGenerator(
   private val schemaProvider: SchemaProvider,
   private val configRepository: CaskConfigRepository
) {


   fun generateSql(queryString: VyneQLQueryString): SqlStatement {
      return generateSqlWithSelect(queryString, "*")
   }

   fun generateSqlCountRecords(queryString: VyneQLQueryString): SqlStatement {
      return generateSqlWithSelect(queryString, "count(*)")
   }

   private fun generateSqlWithSelect(queryString: VyneQLQueryString, select: String): SqlStatement {
      val vyneSchema = schemaProvider.schema()
      val taxiSchema = vyneSchema.taxi
      val query = VyneQlCompiler(queryString, taxiSchema)
         .compile()
         .getOrHandle { errors -> throw CaskBadRequestException(errors.joinToString("\n") { it.detailMessage }) }
      if (query.typesToFind.size != 1) {
         throw CaskBadRequestException("VyneQl queries support exactly one target type.  Found ${query.typesToFind.size}")
      }
      val (collectionTypeName, discoveryType) = assertIsArrayType(query.typesToFind.first())
      val config = getActiveConfig(collectionTypeName)
      // Hmmm... looks like fieldName and columnName are being used interchangably.
      // This will likely hurt us later
      val collectionType = taxiSchema.objectType(collectionTypeName.fullyQualifiedName)
      val criteria = discoveryType.constraints.map { constraintToCriteria(it, collectionType, vyneSchema) }

      val whereClause = if (criteria.isNotEmpty()) {
         "WHERE ${criteria.joinToString(" AND ") { it.sql }}"
      } else ""

      return SqlStatement(
         sql = """SELECT ${select} from ${config.tableName} $whereClause""".trim() + ";",
         params = criteria.flatMap { it.params }
      )

   }


   private fun constraintToCriteria(constraint: Constraint, collectionType: ObjectType, schema: Schema): SqlStatement {
      return when (constraint) {
         is PropertyToParameterConstraint -> buildPropertyConstraintClause(constraint, collectionType, schema)
         else -> error("Error building sql clause for type ${collectionType.qualifiedName} - support for constraint types ${constraint::class.simpleName} is not yet implemented")
      }
   }

   private fun buildPropertyConstraintClause(constraint: PropertyToParameterConstraint, collectionType: ObjectType, schema: Schema): SqlStatement {
      val constraintBuilder = getConstraintBuilder(constraint.propertyIdentifier, collectionType, schema)
      return constraintBuilder.build(constraint)
   }

   private fun getConstraintBuilder(propertyIdentifier: PropertyIdentifier, collectionType: ObjectType, schema: Schema):ConstraintBuilder {
      return when (propertyIdentifier) {
         is PropertyTypeIdentifier -> {
            val fieldReferences = collectionType.fieldReferencesAssignableTo(schema.type(propertyIdentifier.type.fullyQualifiedName).taxiType)
            when (fieldReferences.size) {
               0 ->  throw CaskBadRequestException("No fields on type ${collectionType.qualifiedName} found with type ${propertyIdentifier.taxi}")
               1 -> {
                  val fieldReference = fieldReferences.first()
                  when (fieldReference.path.size) {
                     0 -> error("FieldReference for ${propertyIdentifier.taxi} on type ${collectionType.qualifiedName} should not contain an empty path.")
                     1 -> ColumnConstraintBuilder(fieldReference)
                     else -> JsonConstraintBuilder(fieldReference)
                  }
               }
               else -> throw CaskBadRequestException("Type ${propertyIdentifier.type} is ambiguous on type ${collectionType.qualifiedName} - there are multiple fields with this type: ${fieldReferences.joinToString { it.description }}")
            }
         }
         is PropertyFieldNameIdentifier -> {
            // TODO : MP - This isn't actually supported at the moment (the VyneQL compiler prevents
            // this.fieldName qualifiers, for no good reason).
            // When we come to handle this, check that the value of propertyIdentifier.name.path
            // doesn't contain the  "this." prefix
            val fieldName = propertyIdentifier.name.path
            val field = collectionType.field(fieldName)
            ColumnConstraintBuilder(FieldReference(collectionType, listOf(field)))
         }
      }
   }
   private fun propertyIdentifierToColumnName(propertyIdentifier: PropertyIdentifier, collectionType: ObjectType, schema: Schema): Pair<ColumnName, Field> {
      return when (propertyIdentifier) {
         is PropertyTypeIdentifier -> {
            val fields = collectionType.fieldsWithType(propertyIdentifier.type)
            when (fields.size) {
               0 -> {
                  val fieldName = fieldsWithTypeConsideringFullInheritanceGraph(collectionType, propertyIdentifier, schema)
                  fieldName to  collectionType.field(fieldName)
               }
               1 ->  fields.first().name to fields.first()
               else -> throw CaskBadRequestException("Type ${propertyIdentifier.type} is ambiguous on type ${collectionType.qualifiedName} - there are multiple fields with this type: ${fields.joinToString { it.name }}")
            }
         }
         is PropertyFieldNameIdentifier -> {
            // TODO : MP - This isn't actually supported at the moment (the VyneQL compiler prevents
            // this.fieldName qualifiers, for no good reason).
            // When we come to handle this, check that the value of propertyIdentifier.name.path
            // doesn't contain the  "this." prefix
            val fieldName = propertyIdentifier.name.path
            val field = collectionType.field(fieldName)
            fieldName to field
         }
      }
   }

   private fun fieldsWithTypeConsideringFullInheritanceGraph(collectionType: ObjectType, propertyIdentifier: PropertyIdentifier, schema: Schema): AttributeName {
      val propertyVyneType = schema.type(propertyIdentifier.taxi)
      val collectionVyneType = schema.type(collectionType.qualifiedName)
      val matchingAttributes = collectionVyneType.attributes.filter { (_, field) -> schema.type(field.type).inheritsFrom(propertyVyneType) }.mapNotNull { it.key }
      return when (matchingAttributes.size) {
         0 -> throw CaskBadRequestException("No fields on type ${collectionType.qualifiedName} found with type ${propertyIdentifier.taxi}")
         1 -> matchingAttributes.first()
         else -> throw CaskBadRequestException("Type ${propertyIdentifier.taxi} is ambiguous on type ${collectionType.qualifiedName} - there are multiple fields with this type: ${matchingAttributes.joinToString { it }}")
      }

   }
   //this.schemaProvider.schema().type("ion.orders.FormattedIonOrderEventDate_33231d").inheritsFrom(this.schemaProvider.schema().type("cacib.orders.OrderEventDate"))
   private fun getActiveConfig(collectionType: QualifiedName): CaskConfig {
      val configs = configRepository.findActiveTables(collectionType.fullyQualifiedName)
      if (configs.isEmpty()) {
         throw CaskBadRequestException("No active casks found for type ${collectionType.fullyQualifiedName}")
      }
      if (configs.size > 1) {
         throw CaskBadRequestException("VyneQl queries only support a single active cask at present.  For type ${collectionType.fullyQualifiedName} there were ${configs.size} found")
      }
      return configs.first()
   }

   private fun assertIsArrayType(discoveryType: DiscoveryType): Pair<QualifiedName, DiscoveryType> {
      if (!PrimitiveType.isTypedCollection(discoveryType.type)) {
         val typeName = discoveryType.type.fullyQualifiedName
         throw CaskBadRequestException("VyneQl queries must be for array types - found $typeName, try $typeName[] ")
      }
      return discoveryType.type.parameters[0] to discoveryType
   }
}

interface ConstraintBuilder {
   fun build(constraint: PropertyToParameterConstraint): SqlStatement
}

class ColumnConstraintBuilder(private val fieldReference:FieldReference) : ConstraintBuilder {
   init {
       require(fieldReference.path.size == 1) {"ColumnConstraintBuilder should only be used with a path size of 1"}
   }
   override fun build(constraint: PropertyToParameterConstraint): SqlStatement {
      val field = fieldReference.path.first()
      val columnName = field.name
      val operator = constraint.operator.symbol
      val expected = when(val constraintValue = constraint.expectedValue) {
         is ConstantValueExpression -> CaskDAO.castArgumentToJdbcType(field,constraintValue.value.toString())
         else -> TODO("Handling of constraint type ${constraintValue::class.simpleName} is not yet implemented")
      }

      return SqlStatement("\"$columnName\" $operator ?", listOf(expected))

   }
}

class JsonConstraintBuilder(private val fieldReference: FieldReference) : ConstraintBuilder {
   init {
      require(fieldReference.path.size > 1) {"JsonConstraintBuilder should only be used with a path size > 1"}
   }

   override fun build(constraint: PropertyToParameterConstraint): SqlStatement {
      TODO("Not yet implemented")
   }
}
