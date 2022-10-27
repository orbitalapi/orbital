package io.vyne.query.graph.edges

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.rightIfNotNull
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.QueryContext
import io.vyne.schemas.Relationship

abstract class AttributeEvaluator(override val relationship: Relationship) : EdgeEvaluator {

   protected fun pathToAttribute(edge: EvaluatableEdge): String =
      edge.target.value as String// io.vyne.SomeType/someAttribute

   override suspend fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      val previousValue =
         requireNotNull(edge.previousValue) { "Cannot evaluate $relationship when previous value was null.  Work with me here!" }

      if (previousValue is TypedNull) {
         return edge.failure(
            previousValue,
            "Null was returned from previous edge: " + (edge.previous as EvaluatedEdge).description
         )
      }

      require(previousValue is TypedObject) {
         "Cannot evaluate $relationship when the previous value isn't a TypedObject - got ${previousValue::class.simpleName}"
      }

      // TypedObject has no attributes - service returned no value, returning failure response
      if (previousValue.isEmpty()) {
         return edge.failure(null)
      }

      val previousObject = previousValue
      return getAttributeValue(pathToAttribute(edge), previousObject, context)
         .map { edge.success(it) }
         .getOrHandle { edge.failure(null, it) }

      // 1-Feb: This code used to be here - but unsure when it was authored, or why.
      // However, it breaks tests as when we receive a response with a null value, we tag this
      // as a failed evaluation.  That specifically broke policy evaluation tests, but suspect there
      // were other use cases that are invalid.
      // Reverting to edge.success(attribute) -- (as was on develop) -- resolved the issue.
      // However, I'm leaving this note here so I remember that I've explicitly reverted the below behaviour.
      // when I work out WHY we added it, I'll need to add better tests.

//      val attribute = previousObject[attributeName]
//      return if (attribute is TypedNull) {
//         edge.failure(null, "Attribute $attributeName evaluated to null")
//      } else {
//         edge.success(attribute)
//      }
   }

   protected fun getAttributeValue(
       pathToAttribute: String,
       previousObject: TypedObject,
       context: QueryContext
   ): Either<String, TypedInstance> {
      val pathAttributeParts = pathToAttribute.split("/")
      val attributeName = pathAttributeParts.last()
      val typeName = pathAttributeParts.first()
      val evaluatedToNullErrorMessage = "Attribute $attributeName on type $typeName evaluated to null"
      if (!context.schema.hasType(typeName)) {
         return "Attribute $attributeName declared as unknown type $typeName".left()
      }
      val attributeOrError: Either<String, TypedInstance> = if (previousObject.hasAttribute(attributeName)) {
         previousObject[attributeName].rightIfNotNull { evaluatedToNullErrorMessage }
      } else {
         val entityType = context.schema.type(typeName)
         entityType.attributes[attributeName]?.let { field ->
//            if (field.formula != null) {
//               val calculationResult = CalculatedFieldScanStrategy(CalculatorRegistry())
//                  .tryCalculate(context.schema.type(field.type), context, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
//               calculationResult.rightIfNotNull { evaluatedToNullErrorMessage }
//            } else {
               evaluatedToNullErrorMessage.left()
//            }
         } ?: evaluatedToNullErrorMessage.left()
      }
      return attributeOrError
   }
}
