package io.vyne.query.graph

import io.vyne.models.MixedSources
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.FactDiscoveryStrategy
import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.graph.operationInvocation.UnresolvedOperationParametersException
import io.vyne.schemas.Link
import io.vyne.schemas.Relationship
import io.vyne.schemas.Type
import io.vyne.utils.log
import kotlinx.coroutines.flow.flow


class HasAttributeEvaluator : LinkEvaluator {
   override val relationship: Relationship = Relationship.HAS_ATTRIBUTE

   override suspend fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      assert(startingPoint is TypedObject) { "Cannot evaluate attribute ${link.end} on $startingPoint as it doesn't have any attributes" }
      val startingPointObj = startingPoint as TypedObject
      // Attribute names are passed in a qualified form (Type/attribute).  Just pick the attribute part
      val attributeName = link.end.name.split("/").last()
      assert(startingPointObj.hasAttribute(attributeName)) { "${startingPoint.type.name} doesn't define an attribute called $attributeName" }
      val value = startingPointObj[attributeName] ?: error("$link evaluated to null")
      return EvaluatedLink(link, startingPoint, value)
   }
}

class RequiresParameterEvaluator : LinkEvaluator {
   override val relationship: Relationship = Relationship.REQUIRES_PARAMETER

   override suspend fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      val paramType = context.schema.type(link.end)
      val paramValue = context.getFactOrNull(paramType)
      if (paramValue != null) {
         return EvaluatedLink.success(link, startingPoint, paramValue)
      }
      if (startingPoint.type == paramType) {
         return EvaluatedLink.success(link, startingPoint, startingPoint)
      }
      if (!paramType.isParameterType) {
         throw UnresolvedOperationParametersException("No instance of type ${paramType.name} is present in the graph, and the type is not a parameter type, so cannot be constructed. ", context.evaluatedPath(), context.profiler.root)
      }

      // This is a parameter type.  Try to construct an instance
      val requestObject = attemptToConstruct(paramType, context)
      return EvaluatedLink.success(link, startingPoint, requestObject)
   }

   private suspend fun attemptToConstruct(paramType: Type, context: QueryContext, typesCurrentlyUnderConstruction: Set<Type> = emptySet()): TypedInstance {
      val fields = paramType.attributes.map { (attributeName, field) ->
         val attributeType = context.schema.type(field.type.name)
         val anyDepthExpectOne = context.getFactOrNull(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
         if (anyDepthExpectOne != null) {
            attributeName to anyDepthExpectOne
         } else if (!attributeType.isScalar && !typesCurrentlyUnderConstruction.contains(attributeType)) {
            // TODO : This could be a bad idea.
            // This is ignoring the concept of Parameter types -- so maybe they're not a good idea?
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} not present within the context.  Attempting to construct one.")
            val constructedType = attemptToConstruct(attributeType, context, typesCurrentlyUnderConstruction = typesCurrentlyUnderConstruction + attributeType)
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} constructed: $constructedType")
            attributeName to constructedType
         } else  {
            // TODO : This could cause a stack overflow / infinite loop.
            // Consider making the context aware of what searches are currently taking place,
            // and returning a failed result in the case of a duplicate search
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} not present within the context, and not constructable - initiating a query to attempt to resolve it")
            val queryResult = context.find(QuerySpecTypeNode(attributeType))
            if (!queryResult.isFullyResolved) {
               throw UnresolvedOperationParametersException("Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ", context.evaluatedPath(), context.profiler.root)
            } else {
               attributeName to queryResult[attributeType]!!
            }
         }
      }.toMap()
      return TypedObject(paramType, fields, MixedSources)
   }


}

abstract class PassThroughEvaluator(override val relationship: Relationship) : LinkEvaluator {
   override suspend fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      return EvaluatedLink.success(link, startingPoint, startingPoint)
   }
}

class AttributeOfEvaluator : PassThroughEvaluator(Relationship.IS_ATTRIBUTE_OF)
class IsTypeOfEvaluator : PassThroughEvaluator(Relationship.IS_TYPE_OF)
//class HasParamOfTypeEvaluator : PassThroughEvaluator(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
class OperationParameterEvaluator : PassThroughEvaluator(Relationship.IS_PARAMETER_ON)
