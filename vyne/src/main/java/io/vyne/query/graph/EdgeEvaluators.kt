package io.vyne.query.graph

import com.fasterxml.jackson.annotation.JsonIgnore
import es.usc.citius.hipster.graph.GraphEdge
import io.vyne.formulas.CalculatorRegistry
import io.vyne.models.MixedSources
import io.vyne.models.TypedInstance
import io.vyne.models.TypedNull
import io.vyne.models.TypedObject
import io.vyne.query.CalculatedFieldScanStrategy
import io.vyne.query.FactDiscoveryStrategy
import io.vyne.query.QueryContext
import io.vyne.query.QuerySpecTypeNode
import io.vyne.query.graph.operationInvocation.UnresolvedOperationParametersException
import io.vyne.schemas.Operation
import io.vyne.schemas.Relationship
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.assertingThat
import io.vyne.utils.log
import lang.taxi.types.PrimitiveType


fun GraphEdge<Element, Relationship>.description(): String {
   return "${this.vertex1.valueAsQualifiedName().name} -[${this.edgeValue.description}]-> ${this.vertex2.valueAsQualifiedName().name}"
}

// TODO : Come up with a better name for this...
// I need an interface because I want to be able to recurse backwards,
// but need to have a start point.  Struggling a bit with this right now.
interface PathEvaluation {
   val resultValue: TypedInstance?
   val element: Element
}

data class StartingEdge(
   override val resultValue: TypedInstance,
   override val element: Element
) : PathEvaluation

data class EvaluatableEdge(
   @JsonIgnore
   val previous: PathEvaluation,
   @JsonIgnore
   val relationship: Relationship,
   @JsonIgnore
   val target: Element
) {
   @JsonIgnore
   val vertex1: Element = previous.element

   @JsonIgnore
   val vertex2 = target;

   @JsonIgnore
   val previousValue: TypedInstance? = previous.resultValue

   val description = "${vertex1} -[${relationship}]-> ${vertex2}"

   fun success(value: TypedInstance?): EvaluatedEdge {
      // TODO : Are we adding any value by having "target" here? -- isn't it always vertex2?
      // If so, just remove it - as it's inferrable from 'previous'
      return EvaluatedEdge.success(this, target, value)
   }

   fun failure(value: TypedInstance?): EvaluatedEdge {
      // TODO : Are we adding any value by having "target" here? -- isn't it always vertex2?
      // If so, just remove it - as it's inferrable from 'previous'
      return EvaluatedEdge(this, target, value, "Error")
   }
}

interface EdgeEvaluator {
   val relationship: Relationship
   fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge
}

class RequiresParameterEdgeEvaluator(val parameterFactory: ParameterFactory = ParameterFactory()) : EdgeEvaluator {
   override val relationship: Relationship = Relationship.REQUIRES_PARAMETER

   override fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      if (edge.target.elementType == ElementType.PARAMETER) {
         // Pass through, the next vertex should be the param type
         return EvaluatedEdge.success(edge, edge.vertex2, edge.previousValue)
      }
      assert(edge.vertex2.elementType == ElementType.TYPE, { "RequiresParameter must be evaluated on an element type of Type, received ${edge.description}" })

      // Normally, we'd just use vertex2 to tell us the type.
      // But, because Hispter4J doesn't support identical Vertex Pairs, we can't.
      // (See usages of the parameter() function for more details)
      // So, look up the service directly, and get the parameter type from the schema
      val parts = (edge.vertex1.value as String).split("/").assertingThat({ it.size == 3 })
      val operationReference = parts[0]
      val paramIndex = Integer.parseInt(parts[2])
      val (_, operation) = context.schema.operation(operationReference.fqn())
      val paramType = operation.parameters[paramIndex].type

      val discoveredParam = parameterFactory.discover(paramType, context, operation)
      return EvaluatedEdge.success(edge, instanceOfType(discoveredParam.type), discoveredParam)
//      val paramType = context.schema.type(edge.vertex2.value as String)
   }
}

class ParameterFactory {
   fun discover(paramType: Type, context: QueryContext, operation: Operation? = null): TypedInstance {
      // First, search only the top level for facts
      val firstLevelDiscovery = context.getFactOrNull(paramType, strategy = FactDiscoveryStrategy.TOP_LEVEL_ONLY)
      if (hasValue(firstLevelDiscovery)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         // TODO (2) : Fail if there are multiple instances
         return firstLevelDiscovery!!
      }

      // Check to see if there's exactly one instance somewhere within the context
      // Note : I'm cheating here, I really should find the value required by retracing steps
      // walked in the path.  But, it's unclear if this is possible, given the scattered way that
      // the algorithims are evaluated
      val anyDepthOneDistinct = context.getFactOrNull(paramType, strategy = FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
      if (hasValue(anyDepthOneDistinct)) {
         // TODO (1) : Find an instance that is linked, somehow, rather than just something random
         return anyDepthOneDistinct!!
      }

//      if (startingPoint.type == paramType) {
//         return EvaluatedLink.success(link, startingPoint, startingPoint)
//      }
      if (!paramType.isParameterType) {
         throw UnresolvedOperationParametersException("No instance of type ${paramType.name} is present in the graph, and the type is not a parameter type, so cannot be constructed. ", context.evaluatedPath(), context.profiler.root)
      }

      // This is a parameter type.  Try to construct an instance
      return attemptToConstruct(paramType, context, operation)
   }

   private fun hasValue(instance: TypedInstance?): Boolean {
      return when  {
         instance == null -> false
         instance is TypedNull -> false
         // This is a big call, treating empty strings as not populated
         // It's probably the wrong call.
         // But we need to consider how to filter this data upstream from providers,
         // and allow that filtering to be configurable.
         // Adding this here now because it's caused a bug at a client, let's
         // revisit if/when it becomes problematic.
         instance.type.taxiType.basePrimitive == PrimitiveType.STRING && instance.value == "" -> false
         else -> true
      }
   }

   private fun attemptToConstruct(
      paramType: Type,
      context: QueryContext,
      operation: Operation?,
      typesCurrentlyUnderConstruction: Set<Type> = emptySet()): TypedInstance {
      val fields = paramType.attributes.map { (attributeName, field) ->
         val attributeType = context.schema.type(field.type.fullyQualifiedName)

         // THIS IS WHERE I'M UP TO.
         // Try restructing this to a strategy approach.
         // Can we try searching within the context before we try constructing?
         // what are the impacts?
         var attributeValue: TypedInstance? = context.getFactOrNull(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)

         // First, look in the context to see if it's there.
         if (attributeValue == null) {
            // ... if not, try and find the value in the graph
            context.excludedServices
            val queryResult = context.find(QuerySpecTypeNode(attributeType),  operation?.let { setOf(it) } ?: setOf(operation!!))
            if (queryResult.isFullyResolved) {
               attributeValue = queryResult[attributeType] ?:
                  // TODO : This might actually be legal, as it could be valid for a value to resolve to null
                  throw IllegalArgumentException("Expected queryResult to return attribute with type ${attributeType.fullyQualifiedName} but the returned value was null")
            } else {
               // ... finally, try constructing the value...
               if (!attributeType.isScalar && !typesCurrentlyUnderConstruction.contains(attributeType)) {
                  log().debug("Parameter of type {} not present within the context.  Attempting to construct one.", attributeType.name.fullyQualifiedName)
                  val constructedType = attemptToConstruct(attributeType, context, typesCurrentlyUnderConstruction = typesCurrentlyUnderConstruction + attributeType, operation = operation)
                  log().debug("Parameter of type {} constructed: {}", constructedType, attributeType.name.fullyQualifiedName)
                  attributeValue = constructedType
               }
            }
         }

         if (attributeValue == null) {
            throw UnresolvedOperationParametersException("Unable to construct instance of type ${paramType.name}, as field $attributeName (of type ${attributeType.name}) is not present within the context, and is not constructable ", context.evaluatedPath(), context.profiler.root)
         }

         // else ... attributeValue != null -- we found it.  Good work team, move on.
         attributeName to attributeValue
      }.toMap()
      return TypedObject(paramType, fields, MixedSources)
   }

}


data class EvaluatedEdge(val edge: EvaluatableEdge, val resultGraphElement: Element?, override val resultValue: TypedInstance?, val error: String? = null) : PathEvaluation {
   // TODO : Re-think this.  EvaluatedEdge.element can be null in case of a failure.
   // Therefore, is it correct that "element" is non-null?
   override val element: Element = resultGraphElement!!

   companion object {
      fun success(evaluatedEdge: EvaluatableEdge, result: Element, resultValue: TypedInstance?): EvaluatedEdge {
         return EvaluatedEdge(evaluatedEdge, result, resultValue, error = null)
      }

      fun failed(edge: EvaluatableEdge, error: String): EvaluatedEdge {
         return EvaluatedEdge(edge, null, null, error)
      }
   }

   val wasSuccessful: Boolean = error == null

   fun description(): String {
      var desc = edge.description
      if (wasSuccessful) {
         desc += " (${resultGraphElement!!}) ✔"
      } else {
         desc += " ✘ -> $error"
      }
      return desc
   }

   override fun toString(): String = description()
}

abstract class PassThroughEdgeEvaluator(override val relationship: Relationship) : EdgeEvaluator {
   override fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      return edge.success(edge.previousValue)
   }
}

class AttributeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_ATTRIBUTE_OF)
class IsTypeOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_TYPE_OF)
class HasParamOfTypeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
class OperationParameterEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_PARAMETER_ON)
class IsInstanceOfEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.IS_INSTANCE_OF)
class CanPopulateEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.CAN_POPULATE)
class ExtendsTypeEdgeEvaluator : PassThroughEdgeEvaluator(Relationship.EXTENDS_TYPE)

abstract class AttributeEvaluator(override val relationship: Relationship) : EdgeEvaluator {
   override fun evaluate(edge: EvaluatableEdge, context: QueryContext): EvaluatedEdge {
      val previousValue = requireNotNull(edge.previousValue) { "Cannot evaluate $relationship when previous value was null.  Work with me here!" }

      if (previousValue is TypedNull) {
         return edge.failure(previousValue)
      }

      require(previousValue is TypedObject) {
         "Cannot evaluate $relationship when the previous value isn't a TypedObject - got ${previousValue::class.simpleName}"
      }

      // TypedObject has no attributes - service returned no value, returning failure response
      if (previousValue.isEmpty()) {
         return edge.failure(null)
      }

      val previousObject = previousValue as TypedObject
      val pathToAttribute = edge.target.value as String// io.vyne.SomeType/someAttribute
      val pathAttributeParts = pathToAttribute.split("/")
      val attributeName = pathAttributeParts.last()
      if (!previousObject.hasAttribute(attributeName)) {
         val typeName = pathAttributeParts.first()
         var calculatedValue: EvaluatedEdge? = null
            if (context.schema.hasType(typeName)) {
            val pathType = context.schema.type(typeName)
            pathType.attributes[attributeName]?.let { field ->
               if (field.formula != null) {
                CalculatedFieldScanStrategy(CalculatorRegistry())
                     .tryCalculate(context.schema.type(field.type), context, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE)?.let {
                      calculatedValue = edge.success(it)
                     }
               }

            }
         }
         return calculatedValue ?: edge.failure(null)
      }
      val attribute = previousObject[attributeName]
      return if (attribute is TypedNull) {
         edge.failure(null)
      } else {
         edge.success(attribute)
      }
   }
}

class InstanceHasAttributeEdgeEvaluator : AttributeEvaluator(Relationship.INSTANCE_HAS_ATTRIBUTE)

// Note: I suspect this might cause problems.
// I'm using this because in a solution path, I receive a value from the server, but still end up
// evaluating a HasAttribute, rather than InstanceHasAttribute. (see TradeComplianceTest.canFindTraderMaxValue).
// I have a feeling that I'm going to hit issues here when I'm evluating some paths PRIOR to fetching values.
// TODO.
class HasAttributeEdgeEvaluator : AttributeEvaluator(Relationship.HAS_ATTRIBUTE)
