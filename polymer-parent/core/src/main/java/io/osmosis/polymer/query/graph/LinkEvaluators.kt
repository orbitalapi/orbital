package io.osmosis.polymer.query.graph

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.schemas.Link
import io.osmosis.polymer.schemas.Relationship
import org.springframework.stereotype.Component


@Component
class AttributeOfEvaluator : LinkEvaluator {
   override val relationship: Relationship = Relationship.IS_ATTRIBUTE_OF

   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      assert(startingPoint is TypedObject, { "Cannot evaluate attribute ${link.end} on $startingPoint as it doesn't have any attributes" })
      // TODO Handle getters, and use some form of reflection helper
      val startingPointObj = startingPoint as TypedObject
      val value = startingPointObj[link.end.name]
      TODO("I think this is wrong, and is evaluating HAS_ATTRIBUTE, not IS_ATTRIBUTE_OF")
      return EvaluatedLink(link, startingPoint, value)
   }
}

@Component
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

abstract class PassThroughEvaluator(override val relationship: Relationship) : LinkEvaluator {
   override fun evaluate(link: Link, startingPoint: TypedInstance, context: QueryContext): EvaluatedLink {
      return EvaluatedLink.success(link, startingPoint, startingPoint)
   }
}

@Component
class IsTypeOfEvaluator : PassThroughEvaluator(Relationship.IS_TYPE_OF)

@Component
class OperationParameterEvaluator : PassThroughEvaluator(Relationship.IS_PARAMETER_ON)
