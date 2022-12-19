package io.vyne.cask.query.vyneql

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.findActiveTables
import io.vyne.cask.query.CaskBadRequestException
import io.vyne.cask.query.CaskDAO
import io.vyne.connectors.jdbc.sql.dml.SelectStatementGenerator
import io.vyne.connectors.jdbc.sql.dml.TableNameProvider
import io.vyne.models.toSql
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import lang.taxi.Compiler
import lang.taxi.query.DiscoveryType
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.convertToPropertyConstraint
import lang.taxi.services.operations.constraints.ConstantValueExpression
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import lang.taxi.types.ArrayType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import lang.taxi.types.StreamType
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

data class SqlStatement(val sql: String, val params: List<Any>)
typealias ColumnName = String

@Component
class VyneQlSqlGenerator(
    private val schemaProvider: SchemaProvider,
    private val configRepository: CaskConfigRepository
) {

   fun generateSql(queryString: TaxiQLQueryString): SqlStatement {
      return generateSqlWithSelect(queryString, SelectStatementGenerator.SelectType.Records)
   }

   fun generateSqlCountRecords(queryString: TaxiQLQueryString): SqlStatement {
      return generateSqlWithSelect(queryString, SelectStatementGenerator.SelectType.Count)
   }

   private fun generateSqlWithSelect(
      queryString: TaxiQLQueryString,
      selectType: SelectStatementGenerator.SelectType,
   ): SqlStatement {
      val vyneSchema = schemaProvider.schema
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
      val caskTableNameProvider: TableNameProvider = { type ->
         require(type.qualifiedName == collectionType.qualifiedName) {"Invalid type - expected ${collectionTypeName.parameterizedName}"}
         config.tableName
      }
      val sqlGenerator = SelectStatementGenerator(taxiDocument, caskTableNameProvider)
      val (statement, params) = sqlGenerator.selectSqlWithIndexedParams(query, DSL.using(SQLDialect.POSTGRES), selectType)
      return SqlStatement(
         sql = statement,
         params = params.map { it.value }
      )

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

      if (!ArrayType.isTypedCollection(discoveryType.typeName) && !StreamType.isStreamTypeName(discoveryType.typeName)) {
         val typeName = discoveryType.typeName.fullyQualifiedName
         throw CaskBadRequestException("VyneQl queries must be for array types - found $typeName, try $typeName[] ")
      }
      return discoveryType.typeName.parameters[0] to discoveryType
   }
}
