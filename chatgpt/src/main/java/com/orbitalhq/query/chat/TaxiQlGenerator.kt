package com.orbitalhq.query.chat

import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.types.TaxiStatementGenerator
import mu.KotlinLogging

object TaxiQlGenerator {

   private val logger = KotlinLogging.logger {}

   fun convertToTaxi(query: ChatGptQuery, schema: Schema): TaxiQLQueryString {
      if (query.taxi != null) {
         return query.taxi
      } else {
         require(query.structuredQuery != null) { "Expected either taxi or a structure query" }
         val findClause = buildFindClause(query.structuredQuery.conditions, schema)
         val projection = buildProjection(query.structuredQuery, findClause.returnsCollection)
         return """${findClause.taxi}
         |$projection
      """.trimMargin().trim()
      }

   }

   private fun buildProjection(query: StructuredQuery, returnsCollection: Boolean): String {
      val collectionPostfix = if (returnsCollection) "[]" else ""
      return query.fields.joinToString(
         separator = "\n",
         prefix = "as {\n",
         postfix = "\n}$collectionPostfix"
      ) { requestedField ->
         val qualifiedName = requestedField.fqn()
         "   ${qualifiedName.name} : ${qualifiedName.parameterizedName}"
      }

   }

   data class FindClause(
      val taxi: String,
      val returnsCollection: Boolean
   )
   private fun buildFindClause(conditions: List<Condition>, schema: Schema): FindClause {
      // Hack choice:
      // Doing a find {} requires an entry point data type,
      // which isn't really ideal, as we're instantly coupled to a model,
      // and requires a user to know which entity to define conditions against
      //
      // This is a broader problem in the design of query syntax, and probably
      // needs to evolve towards find { ... } where { ... }, and then
      // the compiler needs to resolve the "best" starting point in a query.
      // For now, we're hacking this in.
      // Find the models that contain the scalars that our conditions are
      // defined against.

      val generators = conditions.mapNotNull { condition ->
         val typeNames = condition.getTypeNames()
         require(typeNames.size == 1) { "Expected exactly one type in the condition, but found ${typeNames.size} - ${typeNames.joinToString()}" }
         val conditionType = schema.type(typeNames.single())

         val modelsContainingType = schema.types
            .filter { !it.isScalar }
            .filter { it.hasAttributeWithType(conditionType) }

         // If there's a query operation that returns a model that we can express this
         // constraint against, (eg., a database table that contains a column we can filter on using this constraint)
         // then use it
         val convertConditionToContracts = (schema.queryOperations + schema.tableOperations)
            .filter { operation ->
               modelsContainingType.contains(
                  operation.returnType.collectionType ?: operation.returnType
               )
            }
            .map { operation -> ConvertConditionToContract(condition, operation) }


         when {
            convertConditionToContracts.size == 1 -> {
               logger.info { "Converting clause $condition to contract ${condition} " }
               convertConditionToContracts.single()
            }

            else -> {
               logger.info { "Converting clause $condition to given clause ${condition} " }
               ConvertConditionToGiven(condition)
            }
         }
      }

      val given = generators
         .filterIsInstance<ConvertConditionToGiven>()
         .joinToString(separator = ", ") { it.asTaxi() }
         .let { givenStatements ->
            if (givenStatements.isNotEmpty()) {
               "given { $givenStatements }"
            } else ""
         }
      val find = generators
         .filterIsInstance<ConvertConditionToContract>()
         .joinToString { it.asTaxi() }
         .let { find ->
            if (find.isNotEmpty()) {
               "find { $find }"
            } else ""
         }
      val isCollection =  generators
         .filterIsInstance<ConvertConditionToContract>()
         .any { it.isCollection }

      val taxi =  """$given
         |$find
      """.trimMargin()
      return FindClause(taxi,isCollection)
   }

   data class ConvertConditionToGiven(
      val condition: Condition
   ) : TaxiStatementGenerator {
      override fun asTaxi(): String {
         return condition.asTaxi()
      }
   }

   data class ConvertConditionToContract(
      val condition: Condition,
      val operation: RemoteOperation
   ) : TaxiStatementGenerator {

      val isCollection:Boolean = operation.returnType.isCollection
      override fun asTaxi(): String {
         return """${operation.returnType.qualifiedName.shortDisplayName}( ${condition.asTaxi()} )"""
      }
   }


}
