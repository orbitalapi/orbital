package io.vyne.cask.query.vyneql

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.findActiveTables
import io.vyne.cask.query.CaskBadRequestException
import io.vyne.cask.query.CaskDAO
import io.vyne.models.toSql
import io.vyne.schemaApi.SchemaProvider
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import lang.taxi.Compiler
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.ArrayType
import lang.taxi.types.DiscoveryType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import lang.taxi.types.StreamType
import lang.taxi.types.TaxiQLQueryString
import org.springframework.stereotype.Component

data class SqlStatement(val sql: String, val params: List<Any>)
typealias ColumnName = String
@Component
class VyneQlSqlGenerator(
   private val schemaProvider: SchemaProvider,
   private val configRepository: CaskConfigRepository
) {


   fun generateSql(queryString: TaxiQLQueryString, filterSql: String? = null): SqlStatement {
      return generateSqlWithSelect(queryString, "*", filterSql)
   }

   fun generateSqlCountRecords(queryString: TaxiQLQueryString, filterSql: String? = null): SqlStatement {
      return generateSqlWithSelect(queryString, "count(*)", filterSql)
   }

   private fun generateSqlWithSelect(queryString: TaxiQLQueryString, select: String, filterSql: String? = null): SqlStatement {
      val vyneSchema = schemaProvider.schema()
      val taxiDocument = vyneSchema.taxi
      val queries = Compiler(source = queryString, importSources = listOf(taxiDocument)).queries()
      val query = queries.first()

      if (query.typesToFind.size != 1) {
         throw CaskBadRequestException("VyneQl queries support exactly one target type.  Found ${query.typesToFind.size}")
      }
      val (collectionTypeName, discoveryType) = assertIsArrayType(query.typesToFind.first())
      val config = getActiveConfig(collectionTypeName)
      // Hmmm... looks like fieldName and columnName are being used interchangably.
      // This will likely hurt us later
      val collectionType = taxiDocument.objectType(collectionTypeName.fullyQualifiedName)
      val criteria = discoveryType.constraints.map { constraintToCriteria(it, collectionType, vyneSchema) }


      val whereClause = if (criteria.isNotEmpty()) {

         if (filterSql != null) {
            "WHERE ${criteria.joinToString(" AND ") { it.sql }} AND $filterSql"
         } else {
            "WHERE ${criteria.joinToString(" AND ") { it.sql }}"
         }

      } else {
         if (filterSql != null) {
            "WHERE $filterSql"
         } else {
            ""
         }

      }

      return SqlStatement(
         sql = """SELECT $select from ${config.tableName} $whereClause""".trim() + ";",
         params = criteria.flatMap { it.params }
      )

   }

   fun toCaskTableName(queryString: TaxiQLQueryString):String {
      val vyneSchema = schemaProvider.schema()
      val taxiDocument = vyneSchema.taxi
      val queries = Compiler(source = queryString, importSources = listOf(taxiDocument)).queries()
      val query = queries.first()
      if (query.typesToFind.size != 1) {
         throw CaskBadRequestException("VyneQl queries support exactly one target type.  Found ${query.typesToFind.size}")
      }



      //Type(discoveryType.type).taxiType
      query.typesToFind.first()


      val (collectionTypeName, discoveryType) = assertIsArrayType(query.typesToFind.first())
      val config = getActiveConfig(collectionTypeName)
      return config.tableName
   }

   fun toType(queryString: TaxiQLQueryString):String {
      val vyneSchema = schemaProvider.schema()
      val taxiDocument = vyneSchema.taxi

      val queries = Compiler(source = queryString, importSources = listOf(taxiDocument)).queries()
      val query = queries.first()

      if (query.typesToFind.size != 1) {
         throw CaskBadRequestException("VyneQl queries support exactly one target type.  Found ${query.typesToFind.size}")
      }
      val (collectionTypeName, discoveryType) = assertIsArrayType(query.typesToFind.first())
      val config = getActiveConfig(collectionTypeName)
      return config.tableName
   }


   private fun constraintToCriteria(constraint: Constraint, collectionType: ObjectType, schema: Schema): SqlStatement {
      return when (constraint) {
         is PropertyToParameterConstraint -> buildPropertyConstraintClause(constraint, collectionType, schema)
         else -> error("Error building sql clause for type ${collectionType.qualifiedName} - support for constraint types ${constraint::class.simpleName} is not yet implemented")
      }
   }

   private fun buildPropertyConstraintClause(constraint: PropertyToParameterConstraint, collectionType: ObjectType, schema: Schema): SqlStatement {
      val (columnName,field) = propertyIdentifierToColumnName(constraint.propertyIdentifier, collectionType, schema)
      val operator = constraint.operator.toSql()
      val expected = when(val constraintValue = constraint.expectedValue) {
         is ConstantValueExpression -> CaskDAO.castArgumentToJdbcType(field,constraintValue.value.toString())
         else -> TODO("Handling of constraint type ${constraintValue::class.simpleName} is not yet implemented")
      }

      return SqlStatement("\"$columnName\" $operator ?", listOf(expected))

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

      if (!ArrayType.isTypedCollection(discoveryType.type) && !StreamType.isStreamTypeName(discoveryType.type)) {
         val typeName = discoveryType.type.fullyQualifiedName
         throw CaskBadRequestException("VyneQl queries must be for array types - found $typeName, try $typeName[] ")
      }
      return discoveryType.type.parameters[0] to discoveryType
   }
}
