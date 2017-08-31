package io.osmosis.polymer

import es.usc.citius.hipster.graph.GraphBuilder
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

enum class ElementType {
   TYPE,
   MEMBER,
   SERVICE,
   INSTANCE,
   PARAMETER;

   override fun toString(): String {
      return super.toString().toLowerCase().capitalize()
   }
}


data class Element(val value: Any, val elementType: ElementType) {

   fun graphNode(): Element {
      return if (this.elementType == ElementType.INSTANCE) {
         val typeName = (value as TypedInstance).type.name.fullyQualifiedName
         Element(typeName, ElementType.TYPE)
      } else {
         this
      }
   }

   fun valueAsQualifiedName(): QualifiedName {
      return value.toString().fqn()
   }

   override fun toString(): String {
      // Use  graphNode() so we don't dump the full value to the logs, which is boring to read
      return "$elementType(${graphNode().value})"
   }
}

fun type(name: String) = Element(name, ElementType.TYPE)
fun type(type: Type) = type(type.fullyQualifiedName)
fun member(name: String) = Element(name, ElementType.MEMBER)
fun parameter(operationName: String, index: Int) = Element("$operationName/param/$index", ElementType.PARAMETER)
fun operation(name: String) = Element(name, ElementType.SERVICE)
fun instance(value: TypedInstance) = Element(value, ElementType.INSTANCE)

class PolymerGraphBuilder(val schema: Schema) {
   fun build(facts: Set<TypedInstance> = emptySet()): HipsterDirectedGraph<Element, Relationship> {
      val builder = GraphBuilder.create<Element, Relationship>()
      appendTypes(builder, schema)
      appendServices(builder, schema)
      appendInstances(builder, facts, schema)
      return builder.createDirectedGraph()
   }

   private fun appendInstances(builder: GraphBuilder<Element, Relationship>, facts: Set<TypedInstance>, schema: Schema) {
      facts.forEach { typedInstance ->
         builder.connect(instance(typedInstance)).to(type(typedInstance.type)).withEdge(Relationship.IS_INSTANCE_OF)
      }
   }

   private fun appendTypes(builder: GraphBuilder<Element, Relationship>, schema: Schema) {
      schema.types.forEach { type: Type ->

         val typeFullyQualifiedName = type.fullyQualifiedName
         val typeNode = type(typeFullyQualifiedName)
         type.attributes.map { (attributeName, attributeType) ->
            val attributeQualifiedName = "$typeFullyQualifiedName/$attributeName"
            val attributeNode = member(attributeQualifiedName)
            builder.connect(typeNode).to(attributeNode).withEdge(Relationship.HAS_ATTRIBUTE)

            // (attribute) -[IS_ATTRIBUTE_OF]-> (type)
            builder.connect(attributeNode).to(typeNode).withEdge(Relationship.IS_ATTRIBUTE_OF)

            val attributeTypeNode = type(attributeType.fullyQualifiedName)
            builder.connect(attributeNode).to(attributeTypeNode).withEdge(Relationship.IS_TYPE_OF)
            builder.connect(attributeTypeNode).to(attributeNode).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
         }
         log().debug("Added attribute ${type.name} to graph")
      }
   }

   private fun appendServices(builder: GraphBuilder<Element, Relationship>, schema: Schema) {
      return schema.services.forEach { service: Service ->
         service.operations.forEach { operation: Operation ->
            val operationReference = "${service.qualifiedName}@@${operation.name}"
            val operationNode = operation(operationReference)
            operation.parameters.forEachIndexed { index, parameter ->
               // When building services, we need to use 'connector nodes'
               // as Hipster4J doesn't support identical vertex pairs with seperate edges.
               // eg: Service -[requiresParameter]-> Money && Service -[Provides]-> Money
               // isn't supported, and results in the Edge for the 2nd pair to remain undefined
               val typeFqn = parameter.type.fullyQualifiedName
               builder.connect(operationNode).to(parameter(operationReference, index)).withEdge(Relationship.REQUIRES_PARAMETER)
               builder.connect(parameter(operationReference, index)).to(type(typeFqn)).withEdge(Relationship.REQUIRES_PARAMETER)

               builder.connect(type(typeFqn)).to(parameter(operationReference, index)).withEdge(Relationship.IS_PARAMETER_ON)
               builder.connect(parameter(operationReference, index)).to(operationNode).withEdge(Relationship.IS_PARAMETER_ON)
            }
            val resultTypeFqn = operation.returnType.fullyQualifiedName
            builder.connect(operationNode).to(type(resultTypeFqn)).withEdge(Relationship.PROVIDES)
            log().debug("Added operation $operationReference to graph")
         }
      }
   }

}
