package io.osmosis.polymer.query.graph

import es.usc.citius.hipster.graph.GraphEdge
import io.osmosis.polymer.Element
import io.osmosis.polymer.ElementType
import io.osmosis.polymer.instance
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.query.FactDiscoveryStrategy
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QuerySpecTypeNode
import io.osmosis.polymer.query.graph.operationInvocation.UnresolvedOperationParametersException
import io.osmosis.polymer.schemas.Relationship
import io.osmosis.polymer.schemas.Type
import io.osmosis.polymer.schemas.fqn
import io.osmosis.polymer.utils.assertingThat
import io.osmosis.polymer.utils.log


interface EdgeEvaluator {
   val relationship: Relationship
   fun evaluate(edge: GraphEdge<Element, Relationship>, context: QueryContext): EvaluatedEdge
}

class RequiresParameterEdgeEvaluator(val parameterFactory: ParameterFactory = ParameterFactory()) : EdgeEvaluator {
   override val relationship: Relationship = Relationship.REQUIRES_PARAMETER

   override fun evaluate(edge: GraphEdge<Element, Relationship>, context: QueryContext): EvaluatedEdge {
      if (edge.vertex2.elementType == ElementType.PARAMETER) {
         // Pass through, the next vertex should be the param type
         return EvaluatedEdge.success(edge, edge.vertex2)
      }
      assert(edge.vertex2.elementType == ElementType.TYPE, { "RequiresParameter must be evaluated on an element type of Type, received ${edge.vertex1} -[${edge.edgeValue}]-> ${edge.vertex2}" })

      // Normally, we'd just use vertex2 to tell us the type.
      // But, because Hispter4J doesn't support identical Vertex Pairs, we can't.
      // (See usages of the parameter() function for more details)
      // So, look up the service directly, and get the parameter type from the schema
      val parts = (edge.vertex1.value as String).split("/").assertingThat({ it.size == 3 })
      val operationReference = parts[0]
      val paramIndex = Integer.parseInt(parts[2])
      val (_, operation) = context.schema.operation(operationReference.fqn())
      val paramType = operation.parameters[paramIndex].type

      val discoveredParam = parameterFactory.discover(paramType, context)
      return EvaluatedEdge.success(edge, instance(discoveredParam))
//      val paramType = context.schema.type(edge.vertex2.value as String)
   }
}

class ParameterFactory {
   fun discover(paramType: Type, context: QueryContext): TypedInstance {
      // First, search only the top level for facts
      if (context.hasFactOfType(paramType, strategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         // TODO (2) : Fail if there are multiple instances
         return context.getFact(paramType)
      }

      // Check to see if there's exactly one instance somewhere within the context
      // Note : I'm cheating here, I really should find the value requried by retracing steps
      // walked in the path.  But, it's unclear if this is possible, given the scattered way that
      // the algorithims are evaluated
      if (context.hasFactOfType(paramType, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         return context.getFact(paramType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      }

//      if (startingPoint.type == paramType) {
//         return EvaluatedLink.success(link, startingPoint, startingPoint)
//      }
      if (!paramType.isParameterType) {
         throw UnresolvedOperationParametersException("No instance of type ${paramType.name} is present in the graph, and the type is not a parameter type, so cannot be constructed. ", context.evaluatedPath())
      }

      // This is a parameter type.  Try to construct an instance
      val requestObject = attemptToConstruct(paramType, context)
      return requestObject
   }

   private fun attemptToConstruct(paramType: Type, context: QueryContext, typesCurrentlyUnderConstruction: Set<Type> = emptySet()): TypedInstance {
      val fields = paramType.attributes.map { (attributeName, attributeTypeRef) ->
         val attributeType = context.schema.type(attributeTypeRef.name)
         if (context.hasFactOfType(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)) {
            attributeName to context.getFact(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
         } else if (!attributeType.isScalar && !typesCurrentlyUnderConstruction.contains(attributeType)) {
            // TODO : This could be a bad idea.
            // This is ignoring the concept of Parameter types -- so maybe they're not a good idea?
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} not present within the context.  Attempting to construct one.")
            val constructedType = attemptToConstruct(attributeType, context, typesCurrentlyUnderConstruction = typesCurrentlyUnderConstruction + attributeType)
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} constructed: $constructedType")
            attributeName to constructedType
         } else {
            // TODO : This could cause a stack overflow / infinite loop.
            // Consider making the context aware of what searches are currently taking place,
            // and returning a failed result in the case of a duplicate search
            log().debug("Parameter of type ${attributeType.name.fullyQualifiedName} not present within the context, and not constructable - initiating a query to attempt to resolve it")
            val queryResult = context.find(QuerySpecTypeNode(attributeType), context.facts)
            if (!queryResult.isFullyResolved) {
               throw UnresolvedOperationParametersException("Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ", context.evaluatedPath())
            } else {
               attributeName to queryResult[attributeType]!!
            }
         }
      }.toMap()
      return TypedObject(paramType, fields)
   }

}


data class EvaluatedEdge(val edge: GraphEdge<Element, Relationship>, val result: Element?, val error: String? = null) {
   companion object {
      fun success(edge: GraphEdge<Element, Relationship>, result: Element): EvaluatedEdge {
         return EvaluatedEdge(edge, result, null)
      }

      fun failed(edge: GraphEdge<Element, Relationship>, error: String): EvaluatedEdge {
         return EvaluatedEdge(edge, null, error)
      }
   }

   val elements: Set<Element>
      get() {
         val elements = setOf(edge.vertex1, edge.vertex2)
         return if (result == null) elements else elements + result
      }

   val wasSuccessful: Boolean = error == null

   fun description(): String {
      var desc = "${edge.vertex1} -[${edge.edgeValue.description}]-> ${edge.vertex2}"
      if (wasSuccessful) {
         desc += " (${result!!}) ✔"
      } else {
         desc += " ✘ -> $error"
      }
      return desc
   }
}

abstract class PassThroughEdgeEvaluator(override val relationship: Relationship) : EdgeEvaluator {
   override fun evaluate(edge: GraphEdge<Element, Relationship>, context: QueryContext): EvaluatedEdge {
      return EvaluatedEdge.success(edge, edge.vertex2)
   }
}

class AttributeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_ATTRIBUTE_OF)
class IsTypeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_TYPE_OF)
class HasParamOfTypeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
class InstanceHasAttributeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.INSTANCE_HAS_ATTRIBUTE)
class OperationParameterEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_PARAMETER_ON)
class HasAttributeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.HAS_ATTRIBUTE)
class IsInstanceOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_INSTANCE_OF)
