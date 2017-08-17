package io.osmosis.polymer.query.graph

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.query.FactDiscoveryStrategy
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QuerySpecTypeNode
import io.osmosis.polymer.query.graph.operationInvocation.UnresolvedOperationParametersException
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.Type
import io.osmosis.polymer.utils.log


class AttributeOfEvaluator : PassThroughEvaluator(Relationship.IS_ATTRIBUTE_OF) {
//   override val relationship: Relationship = Relationship.IS_ATTRIBUTE_OF
//
//   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
//      assert(startingPoint is TypedObject, { "Cannot evaluate attribute ${link.end} on $startingPoint as it doesn't have any attributes" })
//      // TODO Handle getters, and use some form of reflection helper
//      val startingPointObj = startingPoint as TypedObject
//      val value = startingPointObj[link.end.name]
//      TODO("I think this is wrong, and is evaluating HAS_ATTRIBUTE, not IS_ATTRIBUTE_OF")
//      return EvaluatedLink(link, startingPoint, value)
//   }
}

class HasAttributeEvaluator : LinkEvaluator {
   override val relationship: Relationship = Relationship.HAS_ATTRIBUTE

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      assert(startingPoint is TypedObject, { "Cannot evaluate attribute ${link.end} on $startingPoint as it doesn't have any attributes" })
      val startingPointObj = startingPoint as TypedObject
      // Attribute names are passed in a qualified form (Type/attribute).  Just pick the attribute part
      val attributeName = link.end.name.split("/").last()
      assert(startingPointObj.hasAttribute(attributeName), { "${startingPoint.type.name} doesn't define an attribute called $attributeName" })
      val value = startingPointObj[attributeName] ?: error("$link evaluated to null")
      return EvaluatedLink(link, startingPoint, value)
   }
}

class RequiresParameterEvaluator : LinkEvaluator {
   override val relationship: Relationship = Relationship.REQUIRES_PARAMETER

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      val paramType = context.schema.type(link.end)
      if (context.hasFactOfType(paramType)) {
         return EvaluatedLink.success(link, startingPoint, context.getFact(paramType))
      }
      if (startingPoint.type == paramType) {
         return EvaluatedLink.success(link, startingPoint, startingPoint)
      }
      if (!paramType.isParameterType) {
         throw UnresolvedOperationParametersException("No instance of type ${paramType.name} is present in the graph, and the type is not a parameter type, so cannot be constructed. ")
      }

      // This is a parameter type.  Try to construct an instance
      val requestObject = attemptToConstruct(paramType, context)
      return EvaluatedLink.success(link, startingPoint, requestObject)
   }

   private fun attemptToConstruct(paramType: Type, context: QueryContext, typesCurrentlyUnderConstruction: Set<Type> = emptySet()): TypedInstance {
      val fields = paramType.attributes.map { (attributeName, attributeTypeRef) ->
         val attributeType = context.schema.type(attributeTypeRef.name)
         if (context.hasFactOfType(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)) {
            attributeName to context.getFact(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)
         } else if (!attributeType.isScalar && !typesCurrentlyUnderConstruction.contains(attributeType)) {
            // TODO : This could be a bad idea.
            // This is ignoring the concept of Parameter types -- so maybe they're not a good idea?
            log().debug("Parameter of type $attributeType not present within the context.  Attempting to construct one.")
            val constructedType = attemptToConstruct(attributeType, context, typesCurrentlyUnderConstruction = typesCurrentlyUnderConstruction + attributeType)
            attributeName to constructedType
         } else {
            // TODO : This could cause a stack overflow / infinite loop.
            // Consider making the context aware of what searches are currently taking place,
            // and returning a failed result in the case of a duplicate search
            val queryResult = context.find(QuerySpecTypeNode(attributeType))
            if (!queryResult.isFullyResolved) {
               throw UnresolvedOperationParametersException("Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ")
            } else {
               attributeName to queryResult[attributeType]!!
            }
         }
      }.toMap()
      return TypedObject(paramType, fields)
   }


}

abstract class PassThroughEvaluator(override val relationship: Relationship) : LinkEvaluator {
   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      return EvaluatedLink.success(link, startingPoint, startingPoint)
   }
}

class IsTypeOfEvaluator : PassThroughEvaluator(Relationship.IS_TYPE_OF)

class OperationParameterEvaluator : PassThroughEvaluator(Relationship.IS_PARAMETER_ON)
