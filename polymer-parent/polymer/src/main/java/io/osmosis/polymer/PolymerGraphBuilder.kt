package io.osmosis.polymer

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import es.usc.citius.hipster.graph.HipsterDirectedGraph
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.*
import io.osmosis.polymer.utils.log

enum class ElementType {
   TYPE,
   MEMBER,
   SERVICE,
   // An instance is something we have a real actual instance of.
   // These are available before a search is commenced.
//   INSTANCE,
   // An instance of a type, expected to be discovered via the search,
//    but is not known at the start of the search.
   TYPE_INSTANCE,

   // Keep MEMBER and PROVIDED_INSTANCE_MEMBER seperate, as
   // in many cases we ONLY want to traverse from a member where actually have
   // been given an instance.  If we treat them as the same elementType, then links
   // will get created between nodes forming incorrect paths.
   // (Note - that's a theory, I haven't tested it, so this could be over complicating)
   PROVIDED_INSTANCE_MEMBER,
   PARAMETER;

   override fun toString(): String {
      return super.toString().toLowerCase().capitalize()
   }
}


data class Element(val value: Any, val elementType: ElementType, val instanceValue:Any? = null) {

   fun graphNode(): Element {
//      return if (this.elementType == ElementType.INSTANCE) {
//         val typeName = (value as TypedInstance).type.name.fullyQualifiedName
//         Element(typeName, ElementType.TYPE)
//      } else {
//         this
//      }
      return this
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
fun parameter(paramTypeFqn:String) = Element("param/$paramTypeFqn", ElementType.PARAMETER)
fun operation(name: String) = Element(name, ElementType.SERVICE)
fun providedInstance(name: String, value:Any? = null) = Element(name, ElementType.TYPE_INSTANCE, value)
fun providedInstanceMember(name: String) = Element(name, ElementType.PROVIDED_INSTANCE_MEMBER)
fun instance(value: TypedInstance) = providedInstance(value.type.fullyQualifiedName) // Element(value.type.fullyQualifiedName, ElementType.TYPE_INSTANCE, value)

typealias TypeElement = Element
typealias MemberElement = Element

class PolymerGraphBuilder(val schema: Schema) {
   fun build(facts: Set<TypedInstance> = emptySet()): HipsterDirectedGraph<Element, Relationship> {
      val builder = HipsterGraphBuilder.create<Element, Relationship>()
      val typesAndWhereTheyreUsed = appendTypes(builder, schema)
      appendServices(builder, schema, typesAndWhereTheyreUsed)
      appendInstances(builder, facts, schema, typesAndWhereTheyreUsed)
      return builder.createDirectedGraph()
   }

   private fun appendInstances(builder: HipsterGraphBuilder<Element, Relationship>, facts: Set<TypedInstance>, schema: Schema, typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement>) {
      facts.forEach { typedInstance ->
         val typeFqn = typedInstance.type.fullyQualifiedName
         appendProvidedInstances(builder,typeFqn,schema)
//         val providedInstance = providedInstance(typeFqn)
////         val instance = instance(typedInstance)
//         builder.connect(providedInstance).to(type(typedInstance.type)).withEdge(Relationship.IS_INSTANCE_OF)
//         builder.connect(providedInstance).to(parameter(typeFqn)).withEdge(Relationship.CAN_POPULATE)
//
//         appendInstanceAttributes(schema, typeFqn,builder,providedInstance)
      }
   }

   private fun appendTypes(builder: HipsterGraphBuilder<Element, Relationship>, schema: Schema): Multimap<TypeElement, MemberElement> {
      val typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement> = ArrayListMultimap.create()
      schema.types.forEach { type: Type ->

         val typeFullyQualifiedName = type.fullyQualifiedName
         val typeNode = type(typeFullyQualifiedName)
         type.attributes.map { (attributeName, attributeType) ->
            val attributeQualifiedName = attributeFqn(typeFullyQualifiedName, attributeName)
            val attributeNode = member(attributeQualifiedName)
            builder.connect(typeNode).to(attributeNode).withEdge(Relationship.HAS_ATTRIBUTE)

            // (attribute) -[IS_ATTRIBUTE_OF]-> (type)
            builder.connect(attributeNode).to(typeNode).withEdge(Relationship.IS_ATTRIBUTE_OF)

            val attributeTypeNode = type(attributeType.fullyQualifiedName)
            builder.connect(attributeNode).to(attributeTypeNode).withEdge(Relationship.IS_TYPE_OF)
            typesAndWhereTheyreUsed.put(attributeTypeNode, attributeNode)
            // See the relationship for why commented out ....
            // migrating this relationship to an INSTNACE_OF node.
//            builder.connect(attributeTypeNode).to(attributeNode).withEdge(Relationship.TYPE_PRESENT_AS_ATTRIBUTE_TYPE)
         }
         log().debug("Added attribute ${type.name} to graph")
      }
      return typesAndWhereTheyreUsed
   }

   private fun attributeFqn(typeFullyQualifiedName: String, attributeName: AttributeName): String {
      return "$typeFullyQualifiedName/$attributeName"
   }

   private fun appendServices(builder: HipsterGraphBuilder<Element, Relationship>, schema: Schema, typesAndWhereTheyreUsed: Multimap<TypeElement, MemberElement>) {
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
               val paramNode = parameter(typeFqn)
               builder.connect(operationNode).to(paramNode).withEdge(Relationship.REQUIRES_PARAMETER)
               builder.connect(paramNode).to(operationNode).withEdge(Relationship.IS_PARAMETER_ON)

               if (parameter.type.isParameterType) {
                  // Traverse into the attributes of param types, and add extra nodes.
                  // As we're allowed to instantiate param types, discovered values within the graph
                  // can be used to populate new instances, so form links.
                  parameter.type.attributes.forEach { attriubteName, typeRef ->
                     // Point back to the "parent" param node (the parameterObject)
                     // might revisit this in the future, and point back to the operation itself.
                     builder.connect(parameter(typeRef.fullyQualifiedName)).to(paramNode).withEdge(Relationship.IS_PARAMETER_ON)
                  }
               }
            }

            // Build the instance.
            // It connects to it's type, but also to the attributes that are
            // now traversable, as we have an actual instance of the thing
            val resultInstanceFqn = operation.returnType.fullyQualifiedName
            appendProvidedInstances(builder, resultInstanceFqn, schema, operationNode)


            log().debug("Added operation $operationReference to graph")
         }
      }
   }

   /**
    * Builds all the providedInstance() nodes required for modelling a returned instance from a service.
    * It's return type is created as an instance:type, and all the parameters of the return type
    * are also mapped as providedInstanceMembers() and instance:types.
    */
   private fun appendProvidedInstances(builder: HipsterGraphBuilder<Element, Relationship>, instanceFqn: String, schema: Schema, provider:Element? = null) {
      val providedInstance = providedInstance(instanceFqn)
      if (provider != null) {
         builder.connect(provider).to(providedInstance).withEdge(Relationship.PROVIDES)
      }
      builder.connect(providedInstance).to(type(instanceFqn)).withEdge(Relationship.IS_INSTANCE_OF)
      builder.connect(providedInstance).to(parameter(instanceFqn)).withEdge(Relationship.CAN_POPULATE)

      appendInstanceAttributes(schema, instanceFqn, builder, providedInstance)
   }

   private fun appendInstanceAttributes(schema: Schema, instanceFqn: String,  builder: HipsterGraphBuilder<Element, Relationship>, providedInstance: Element) {
      schema.type(instanceFqn).attributes.forEach { attributeName, typeReference ->
         val providedInstanceMember = providedInstanceMember(attributeFqn(instanceFqn, attributeName))
         builder.connect(providedInstance).to(providedInstanceMember).withEdge(Relationship.INSTANCE_HAS_ATTRIBUTE)
         // The "providedInstance" node of the member itself
         val memberInstance = providedInstance(typeReference.fullyQualifiedName)
         builder.connect(providedInstanceMember).to(memberInstance).withEdge(Relationship.IS_ATTRIBUTE_OF)
         // The member instance we have can populate required params
         builder.connect(memberInstance).to(parameter(typeReference.fullyQualifiedName)).withEdge(Relationship.CAN_POPULATE)
         builder.connect(memberInstance).to(type(typeReference.fullyQualifiedName)).withEdge(Relationship.IS_INSTANCE_OF)
      }
   }

}
