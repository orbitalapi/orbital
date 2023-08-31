package io.vyne.connectors.aws.dynamodb

import io.vyne.connectors.getTypesToFind
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Schema
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.DiscoveryType
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.Field
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import mu.KotlinLogging
import software.amazon.awssdk.services.dynamodb.model.*

class DynamoDbRequestBuilder {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun buildQuery(schema: Schema, taxiQuery: TaxiQLQueryString): DynamoDbRequest {
        val (query, options) = schema.parseQuery(taxiQuery)
        val taxiSchema = schema.taxi
        val typesToFind = getTypesToFind(query, taxiSchema)
        if (typesToFind.size > 1) {
            error("Selecting from multiple types is not supported - found types ${typesToFind.joinToString { it.first.qualifiedName }} in query $query")
        }
        val (typeToFind: ObjectType, discoveryType: DiscoveryType) = typesToFind.single()
        val tableNamesFromType: Map<Type, String> = getTableNames(typesToFind)
        if (tableNamesFromType.size > 1) {
            error("Joins are not supported - can only select from a single table")
        }
        val tableName = tableNamesFromType.values.single()

        return when {
            discoveryType.constraints.isEmpty() -> createScanRequest(typeToFind, tableName)
            discoveryType.constraints.size == 1 && isPrimaryKeyLookup(
                discoveryType.constraints.single(),
                typeToFind
            ) -> createGetItemRequest(discoveryType, typeToFind, tableName)

            else -> createQueryRequest(discoveryType, typeToFind, tableName)
        }
    }

    private fun createQueryRequest(
        discoveryType: DiscoveryType,
        typeToFind: ObjectType,
        tableName: String
    ): DynamoDbRequest {
        val attributeValues = mutableMapOf<String, AttributeValue>()
        val queryRequestBuilder = discoveryType.constraints.map { constraint ->
            getFieldForConstraint(constraint, typeToFind)
                ?: error("No field is found that can be used to evaluate the constraint ${constraint.asTaxi()}")
        }.foldIndexed(QueryRequest.builder().tableName(tableName)) { index, requestBuilder, pair ->
            val (field, operatorExpression) = pair
            val dynamoDbSymbol = when (operatorExpression.operator) {
                FormulaOperator.Equal -> "="
                else -> operatorExpression.operator.symbol
            }
            val paramName = "param$index"
            attributeValues[paramName] = attributeValue(operatorExpression.literalValue())
            requestBuilder
                .filterExpression("${field.name} $dynamoDbSymbol :$paramName")
        }
        return queryRequestBuilder.expressionAttributeValues(attributeValues)
            .build()
    }

    private fun createScanRequest(typeToFind: ObjectType, tableName: String): DynamoDbRequest {
        return ScanRequest.builder()
            .tableName(tableName)
            .build()
    }

    private fun OperatorExpression.literalValue(): Any {
        return listOf(this.lhs, this.rhs).filterIsInstance<LiteralExpression>().singleOrNull()?.value
            ?: error("No literal expression found in statement ${this.asTaxi()}")
    }

    private fun createGetItemRequest(
        discoveryType: DiscoveryType,
        typeToFind: ObjectType,
        tableName: String
    ): DynamoDbRequest {
        val (field, expression) = getFieldForConstraint(discoveryType.constraints.single(), typeToFind)!!
        val keyValue = expression.literalValue()
        return GetItemRequest.builder()
            .tableName(tableName)
            .key(mapOf(field.name to attributeValue(keyValue)))
            .build()
    }

    private fun attributeValue(value: Any?): AttributeValue {
        if (value == null) {
            return AttributeValue.fromNul(true)
        }
        return when (value) {
            is String -> AttributeValue.builder().s(value).build()
            is Number -> AttributeValue.builder().n(value.toString()).build()
            else -> error("Cannot construct a DynamoDb attribute from type ${value::class.simpleName} - not supported")
        }
    }

    private fun getFieldForConstraint(
        constraint: Constraint,
        typeToFind: ObjectType
    ): Pair<Field, OperatorExpression>? {
        if (constraint !is ExpressionConstraint) return null;
        if (constraint.expression !is OperatorExpression) return null;
        val operatorExpression = constraint.expression as OperatorExpression
        if (operatorExpression.operator != FormulaOperator.Equal) return null;
        val parts = listOf(operatorExpression.lhs, operatorExpression.rhs)
        if (parts.filterIsInstance<TypeExpression>().size != 1) return null;
        if (parts.filterIsInstance<LiteralExpression>().size != 1) return null;
        val constraintType = parts.filterIsInstance<TypeExpression>().single().type

        val matchingFields = typeToFind.fields.filter { it.type.isAssignableTo(constraintType) }
        return when {
            matchingFields.isEmpty() -> {
                logger.warn { "Constraint ${constraint.asTaxi()} cannot be evaluated as no field matches ${constraintType.qualifiedName}" }
                null
            }

            matchingFields.size > 1 -> {
                logger.warn { "Constraint ${constraint.asTaxi()} cannot be evaluated as multiple fields match ${constraintType.qualifiedName}: ${matchingFields.joinToString { it.name }}" }
                null
            }

            else -> {
                val matchingField = matchingFields.single()
                matchingField to operatorExpression
            }

        }

    }

    private fun isPrimaryKeyLookup(constraint: Constraint, typeToFind: ObjectType): Boolean {
        val (field, expression) = getFieldForConstraint(constraint, typeToFind) ?: return false
        val idFields = typeToFind.fields
            .filter { it.annotations.any { annotation -> annotation.qualifiedName == "Id" } }

        if (idFields.isEmpty()) {
            return false
        } else if (idFields.size > 1) {
            logger.warn { "Constructing DynamoDb queries for types with mulitple Id annotations is not supported" }
            return false
        }
        val idField = idFields.single()
        val constraintType = listOf(expression.lhs, expression.rhs).filterIsInstance<TypeExpression>().single().type

        return idField.type.inheritsFrom(constraintType)
    }

    private fun getTableNames(typesToFind: List<Pair<ObjectType, DiscoveryType>>): Map<Type, String> {
        return typesToFind.associate { (type, _) ->
            val table = getDynamoTableFromType(type)
            type to table.tableName
        }
    }

    fun buildPut(schema: Schema, recordToWrite: TypedInstance): PutItemRequest {
        val table = getDynamoTableFromType(
            schema.taxiType(recordToWrite.type.name)
        )
        val attributes = convertToDynamoAttributes(recordToWrite)
        return PutItemRequest.builder()
            .tableName(table.tableName)
            .item(attributes)
            .build()
    }

    private fun convertToDynamoAttributes(recordToWrite: TypedInstance): Map<String, AttributeValue> {
        require(recordToWrite is TypedObject) { "Writes not supported on instances of type ${recordToWrite::class.simpleName}" }
        return recordToWrite.type.attributes.map { (name, field) ->
            val fieldValue = recordToWrite[name]
            name to attributeValue(fieldValue.value)
        }.toMap()
    }
}


