package com.orbitalhq.connectors.nosql.mongodb

import com.orbitalhq.connectors.getTypesToFind
import com.orbitalhq.connectors.nosql.mongodb.MongoBaseInvoker.Companion.MongoIdField
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.TaxiDocument
import lang.taxi.expressions.LiteralExpression
import lang.taxi.expressions.OperatorExpression
import lang.taxi.expressions.TypeExpression
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.operations.constraints.Constraint
import lang.taxi.services.operations.constraints.ExpressionConstraint
import lang.taxi.types.FieldReference
import lang.taxi.types.FormulaOperator
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class MongoCriteriaGenerator(private val taxiSchema: TaxiDocument) {
    constructor(schema: Schema) : this(schema.taxi)


    fun crtieriaFor(query: TaxiQlQuery): List<Criteria> {
        val typesToFind = getTypesToFind(query, taxiSchema)
        val typesToCollectionNames = MongoQueryHelpers.getCollectionNames(typesToFind)
        if (typesToCollectionNames.size > 1) {
            error("Mongo Joins are not yet supported - can only select from a single collection")
        }
        return typesToFind.filter { (_, discoveryType) -> discoveryType.constraints.isNotEmpty() }
            .map { (type, discoveryType) ->
                // Pretty sure that now we'll only ever receive a single constraint,
                // which more often than not is an Operator Constraint.
                // (Which can nest mulitple operator constratins under it)
                require(discoveryType.constraints.size == 1) { "Expected to find a single constraint (which could be a compound operation expression).  Instead, found ${discoveryType.constraints.size}" }

                buildMongoConstraint(
                    type,
                    discoveryType.constraints.single(),
                )
            }


    }



    private fun buildMongoConstraint(
        type: ObjectType,
        constraint: Constraint
    ): Criteria {
        return when (constraint) {
            is ExpressionConstraint -> buildMongoConstraint(
                type,
                constraint
            )

            else -> error("Sql constraints not supported for constraint type ${constraint::class.simpleName}")
        }
    }

    private fun buildMongoConstraint(
        type: ObjectType,
        constraint: ExpressionConstraint
    ): Criteria {
        return when (val expression = constraint.expression) {
            is OperatorExpression -> buildSqlConstraintFromOperatorExpression(
                type,
                expression,
            )

            else -> error("Unsupported expression type: ${expression::class.simpleName}")
        }
    }

    private fun buildSqlConstraintFromOperatorExpression(
        type: ObjectType,
        expression: OperatorExpression
    ): Criteria {
        return when {
            expression.lhs is TypeExpression && expression.rhs is LiteralExpression -> {
                buildTypeToLiteralExpression(
                    expression.lhs as TypeExpression,
                    expression.rhs as LiteralExpression,
                    expression.operator,
                    type
                )
            }

            expression.lhs is OperatorExpression && expression.rhs is OperatorExpression -> {
                buildCompoundExpression(
                    expression.lhs as OperatorExpression,
                    expression.rhs as OperatorExpression,
                    expression.operator,
                    type
                )
            }

            else -> error("Sql generation not implemented for operation expression ${expression.lhs::class.simpleName} and ${expression.rhs::class.simpleName}")
        }
    }

    private fun buildCompoundExpression(
        lhsExpression: OperatorExpression,
        rhsExpression: OperatorExpression,
        operator: FormulaOperator,
        type: ObjectType
    ): Criteria {
        val lhsCriteria = buildSqlConstraintFromOperatorExpression(
            type,
            lhsExpression
        )
        val rhsCriteria = buildSqlConstraintFromOperatorExpression(
            type, rhsExpression
        )
        return when (operator) {
            FormulaOperator.LogicalAnd -> Criteria().andOperator(lhsCriteria, rhsCriteria)
            //lhsCondition.and(rhsCondition)
            FormulaOperator.LogicalOr -> Criteria().orOperator(lhsCriteria, rhsCriteria)
            else -> error("Operator $operator not supported for compound expressions")
        }
    }

    private fun buildTypeToLiteralExpression(
        lhs: TypeExpression,
        rhs: LiteralExpression,
        operator: FormulaOperator,
        type: ObjectType
    ): Criteria {
        val fieldReference = getSingleField(type, lhs.type)
        val fieldName = if (fieldReference.path.single().IdField()) MongoIdField else  fieldReference.path.single().name
        val condition = when (operator) {
            FormulaOperator.Equal -> Criteria.where(fieldName).`is`(rhs.value)
            FormulaOperator.NotEqual -> Criteria.where(fieldName).ne(rhs.value)
            FormulaOperator.LessThan -> Criteria.where(fieldName).lt(rhs.value)
            FormulaOperator.LessThanOrEqual -> Criteria.where(fieldName).lte(rhs.value)
            FormulaOperator.GreaterThan -> Criteria.where(fieldName).gt(rhs.value)
            FormulaOperator.GreaterThanOrEqual -> Criteria.where(fieldName).gte(rhs.value)
            else -> error("$operator is not yet supported in SQL clauses")
        }
        return condition
    }

    private fun getSingleField(sourceType: ObjectType, fieldType: Type): FieldReference {
        val references = sourceType.fieldReferencesAssignableTo(fieldType)
        return when {
            references.isEmpty() -> error("Field ${fieldType.qualifiedName} is not present on type ${sourceType.qualifiedName}")
            references.size == 1 -> {
                val reference = references.single()
                if (reference.path.size == 1) {
                    reference
                } else {
                    error(
                        "${fieldType.qualifiedName} is only accessible on ${sourceType} via a nested property (${
                            reference.path.joinToString(
                                "."
                            )
                        }), which is not supported in SQL statements"
                    )
                }
            }

            else -> error("Field ${fieldType.qualifiedName} is ambiguous on type ${sourceType.qualifiedName} - expected a single match, but found ${references.joinToString { it.description }}")
        }
    }

   companion object {
      fun upsertFor(typedInstance: TypedInstance, documentMap: Map<String, Any?>): Pair<Query, Update>? {
          require(typedInstance is TypedObject) { "Writes not supported on instances of type ${typedInstance::class.simpleName}" }
         val attributeNameForId = idField(typedInstance.type)
         val uniqueIndexFields = uniqueIndexFields(typedInstance.type)
         val uniqueIndexFieldsAndValues =  uniqueIndexFields.map { attributeName ->
               attributeName to typedInstance[attributeName].value
          }.toMap()

          val setOnInsertFields = setOnInsertAnnotations(typedInstance.type)

          var query: Query? = null

          when {
              attributeNameForId != null && typedInstance[attributeNameForId].value != null -> query =
                  Query().addCriteria(Criteria.where(MongoIdField).`is`(typedInstance[attributeNameForId].value))

              uniqueIndexFieldsAndValues.isNotEmpty() && query != null -> uniqueIndexFieldsAndValues.forEach { (attributeName, attributeValue) ->
                  query = query!!.addCriteria(
                      Criteria.where(attributeName).`is`(attributeValue)
                  )
              }

              uniqueIndexFieldsAndValues.isNotEmpty() && query == null ->
                  uniqueIndexFieldsAndValues.forEach { (attributeName, attributeValue) ->
                      if (query == null) {
                          query = Query().addCriteria(Criteria.where(attributeName).`is`(attributeValue))
                      } else {
                          query = query!!.addCriteria(
                              Criteria.where(attributeName).`is`(attributeValue)
                          )
                      }
                  }

              else -> null
          }

         return if (query != null) {
             val documentMapWithoutSetOnUpsertFields = documentMap.filter { entry ->
                 !setOnInsertFields.contains(entry.key)
             }.toMap()
            val update = Update.fromDocument(Document(documentMapWithoutSetOnUpsertFields))
            val updateWithSetOnInsertFields = setOnInsertFields.fold(update) { initial, setOnInsertField ->
                 initial.setOnInsert(setOnInsertField, typedInstance[setOnInsertField].value)
             }
            query!! to updateWithSetOnInsertFields
         } else {
            null
         }
      }

       fun bulkUpsertFor(typedInstance: TypedInstance, documentMap: Map<String, Any?>): Pair<Query, Update>? {
           require(typedInstance is TypedObject) { "Writes not supported on instances of type ${typedInstance::class.simpleName}" }
           val attributeNameForId = idField(typedInstance.type)
           val uniqueIndexFields = uniqueIndexFields(typedInstance.type)
           val uniqueIndexFieldsAndValues =  uniqueIndexFields.map { attributeName ->
               attributeName to typedInstance[attributeName].value
           }.toMap()

           val setOnInsertFields = setOnInsertAnnotations(typedInstance.type)

           var query: Query? = null

           when {
               attributeNameForId != null && typedInstance[attributeNameForId].value != null -> query =
                   Query().addCriteria(Criteria.where(MongoIdField).`is`(typedInstance[attributeNameForId].value))

               uniqueIndexFieldsAndValues.isNotEmpty() && query != null -> uniqueIndexFieldsAndValues.forEach { (attributeName, attributeValue) ->
                   query = query!!.addCriteria(
                       Criteria.where(attributeName).`is`(attributeValue)
                   )
               }

               uniqueIndexFieldsAndValues.isNotEmpty() && query == null ->
                   uniqueIndexFieldsAndValues.forEach { (attributeName, attributeValue) ->
                       if (query == null) {
                           query = Query().addCriteria(Criteria.where(attributeName).`is`(attributeValue))
                       } else {
                           query = query!!.addCriteria(
                               Criteria.where(attributeName).`is`(attributeValue)
                           )
                       }
                   }

               else -> null
           }

           return if (query != null) {
               val documentMapWithoutSetOnUpsertFields = documentMap.filter { entry ->
                   !setOnInsertFields.contains(entry.key)
               }.toMap()
               val update = Update()
               documentMapWithoutSetOnUpsertFields.forEach { entry ->
                   update.set(entry.key, entry.value)
               }
               val updateWithSetOnInsertFields = setOnInsertFields.fold(update) { initial, setOnInsertField ->
                   initial.setOnInsert(setOnInsertField, typedInstance[setOnInsertField].value)
               }
               query!! to updateWithSetOnInsertFields
           } else {
               null
           }
       }

      private fun idField(vyneType: com.orbitalhq.schemas.Type): AttributeName? {
         val idFields = vyneType.getAttributesWithAnnotation("Id".fqn())
         require(idFields.isEmpty() || idFields.size == 1)
         return if (idFields.isEmpty()) {
            null
         } else {
            idFields.keys.first()
         }
      }

       private fun uniqueIndexFields(vyneType: com.orbitalhq.schemas.Type): Set<AttributeName> {
           val idFields = vyneType.getAttributesWithAnnotation(MongoConnector.Annotations.UniqueIndexAnnotationName)
           return if (idFields.isEmpty()) {
               emptySet()
           } else {
               idFields.keys
           }
       }

       private fun setOnInsertAnnotations(vyneType: com.orbitalhq.schemas.Type): Set<AttributeName> {
           val setOnInsertFields = vyneType.getAttributesWithAnnotation(MongoConnector.Annotations.SetOnInsertAnnotationName)
           return setOnInsertFields.keys
       }
   }

}
