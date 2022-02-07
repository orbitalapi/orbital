package io.vyne.query.graph

import es.usc.citius.hipster.algorithm.Algorithm
import es.usc.citius.hipster.algorithm.Hipster
import es.usc.citius.hipster.graph.GraphSearchProblem
import es.usc.citius.hipster.model.impl.WeightedNode
import io.vyne.HipsterGraphBuilder
import io.vyne.VyneGraphBuilderCacheSettings
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.Operation
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.synonymFullyQualifiedName
import lang.taxi.types.Annotatable
import lang.taxi.types.EnumType
import lang.taxi.types.ObjectType
import lang.taxi.types.PrimitiveType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object Algorithms {
   /**
    * Traverses the schema and inspect the schema to find types or fields annotated with the given annotation
    * and return qfn of the type / field type.
    */
   fun findAllTypesWithAnnotation(schema: Schema, taxiAnnotation: String): List<String> {
      return findAllTypesContainingAnnotation(schema, taxiAnnotation).map { it.annotatedType }
   }

   /**
    * Traverses the schema and inspect the schema to find types or fields annotated with the given annotation
    * and return qfn of the type / field type.
    */
   fun findAllTypesContainingAnnotation(schema: Schema, searchTerm: String): List<AnnotationSearchResult> {
      return schema
         .types.flatMap { type ->
            when (val taxiType = type.taxiType) {
               is ObjectType -> findMatchingAnnotationsOnType(taxiType, searchTerm)
               is EnumType -> findMatchingAnnotationsOnEnum(taxiType, searchTerm)
               else -> emptyList()
            }
         }
   }

   private fun findMatchingAnnotationsOnEnum(enumType: EnumType, searchTerm: String): List<AnnotationSearchResult> {
      val matchingTaxiTypes = mutableSetOf<AnnotationSearchResult>()
      if (enumType.hasAnnotationContaining(searchTerm)) {
         matchingTaxiTypes.add(AnnotationSearchResult(enumType.qualifiedName))
      }
      matchingTaxiTypes.addAll(enumType.values.filter { it.hasAnnotationContaining(searchTerm) }
         .map { enumValue ->
            AnnotationSearchResult(enumValue.qualifiedName, enumType.qualifiedName)
         })
      return matchingTaxiTypes.toList()
   }

   private fun findMatchingAnnotationsOnType(taxiType: ObjectType, searchTerm: String): List<AnnotationSearchResult> {
      val matchingTaxiTypes = mutableSetOf<AnnotationSearchResult>()
      if (taxiType.hasAnnotationContaining(searchTerm)) {
         matchingTaxiTypes.add(AnnotationSearchResult(taxiType.qualifiedName))
      }
      taxiType.fields.forEach { taxiField ->
         if (taxiField.hasAnnotationContaining(searchTerm)) {
            matchingTaxiTypes.add(AnnotationSearchResult(taxiField.type.qualifiedName, taxiType.qualifiedName, taxiField.name))
         }
      }
      return matchingTaxiTypes.toList()
   }

   /**
    * Find all operation where the type represented by fullQualifiedName is either an argument or return value.
    * for given fqn 'Foo' return types of 'Foo[]', 'Stream<Foo>' is also considered.
    * note - it also considers the case where the type is an attribute of the return value.
    *
    */
   fun findAllFunctionsWithArgumentOrReturnValueForType(schema: Schema, fullQualifiedName: String): OperationQueryResult {
      val type = schema.type(fullQualifiedName)
      val resultItems = schema.servicesAndOperations().mapNotNull { (service, operation) ->
         val returnType = operation.returnType
         val returnedCollectionMemberType = (operation.returnType.collectionType ?: operation.returnType)
         val attributeQualifiedNames = returnedCollectionMemberType.attributes.map { attribute -> attribute.value.type.parameterizedName }.toSet()
         when {
            returnType.qualifiedName.parameterizedName == type.qualifiedName.parameterizedName ->
               OperationQueryResultItem(service.name, operation.name, operation.qualifiedName, OperationQueryResultItemRole.Output)
            returnedCollectionMemberType.qualifiedName.parameterizedName == type.qualifiedName.parameterizedName ->
               OperationQueryResultItem(service.name, operation.name, operation.qualifiedName, OperationQueryResultItemRole.Output)
            operation.parameters.any{ parameter -> parameter.type.qualifiedName.parameterizedName == type.qualifiedName.parameterizedName } ->
               OperationQueryResultItem(service.name, operation.name, operation.qualifiedName, OperationQueryResultItemRole.Input)
            attributeQualifiedNames.contains(type.qualifiedName.fullyQualifiedName) ->
               OperationQueryResultItem(service.name, operation.name, operation.qualifiedName, OperationQueryResultItemRole.Output)
            else -> null
         }
      }
      return OperationQueryResult(fullQualifiedName, resultItems)
   }

   /**
    * Composition of findAllTypesWithAnnotationDetailed and findAllFunctionsWithArgumentOrReturnValueForType
    */
   fun findAllFunctionsWithArgumentOrReturnValueForAnnotationDetailed(schema: Schema, taxiAnnotation: String): List<Pair<AnnotationSearchResult, OperationQueryResult>> {
      return findAllTypesContainingAnnotation(schema, taxiAnnotation)
         .map { annotationDetail -> Pair(annotationDetail, findAllFunctionsWithArgumentOrReturnValueForType(schema, annotationDetail.annotatedType)) }
   }

   /**
    * Composition of findAllTypesWithAnnotation and findAllFunctionsWithArgumentOrReturnValueForType
    */
   fun findAllFunctionsWithArgumentOrReturnValueForAnnotation(schema: Schema, taxiAnnotation: String): List<OperationQueryResult> {
      return findAllTypesWithAnnotation(schema, taxiAnnotation)
         .map { fqn -> findAllFunctionsWithArgumentOrReturnValueForType(schema, fqn) }
   }

   fun accessibleThroughSingleArgumentFunctionInvocation(schema: Schema, fqn: String): Set<Type> {
      val startingType = schema.type(fqn)
      val solutions = accessibleFromThroughFunctionInvocations(schema, fqn, schema.operationsWithSingleArgument())
      return solutions
         .map { solution -> solution.goalNode }
         .flatMap { it.path() }
         .map { it.state() }
         .filter { it != startingType }
         .toSet()
   }

   /**
    * Identifies the types that are the return value of no-arg operations.
    */
   fun getImmediatelyDiscoverableTypes(schema: Schema) =
      schema.operationsWithNoArgument().map { (_, operation) ->
         operation.returnType.collectionType ?: operation.returnType
      }.toSet()

   /**
    * This function first finds all service operations without any arguments.
    * Next, for each such function it explores all paths starting from function return argument type to return types of other
    * service operations.
    *
    * Example:
    *  model Order {
    *    puid: Puid
    *  }
    *
    *  service MockCaskService {
    *     operation findSingleByPuid( id : Puid ) : Product
    *  }
    *
    *
    *  service OrderService {
    *   operation `findAll`( ) : Order[]
    *  }
    *
    * This function:
    *
    * Step 1. Extract All zero argument operations from the schema -> operation `findAll`( ) : Order[]
    * Step 2: the match 'findAll' has the collection type of 'Order'
    * Step 3: Determine all other operations in the schema -> operation findSingleByPuid( id : Puid ) : Product
    * Step 4: Determine the path from Order to Product, if there is one return it.
    */
   fun immediateDataSourcePaths(schema: Schema): List<Dataset> {
      val typesAccessibleViaNoArgServiceCalls = getImmediatelyDiscoverableTypes(schema)

      val targetTypes = returnTypesOfAllNonZeroArgOperations(schema)

      val graphBuilder = VyneGraphBuilder(schema, VyneGraphBuilderCacheSettings())
      val discoverablePaths = typesAccessibleViaNoArgServiceCalls.map { startingType ->
         pathsForTargets(startingType, schema, targetTypes, graphBuilder)
      }.filter { it.second.isNotEmpty() }
      return discoverablePaths.flatMap { result ->
         result.second.map {
            Dataset(result.first.qualifiedName, it.first.qualifiedName, it.second)
         }
      }
   }

   fun immediateDataSourcePathsFor(schema: Schema, fqn: String): List<Dataset> {
      val startingType = schema.type(fqn)

      val targetTypes = returnTypesOfAllNonZeroArgOperations(schema)

      val graphBuilder = VyneGraphBuilder(schema, VyneGraphBuilderCacheSettings())
      val solution = pathsForTargets(startingType, schema, targetTypes, graphBuilder)
      return solution.second.map {
         Dataset(solution.first.qualifiedName, it.first.qualifiedName, it.second)
      }

   }

   private fun returnTypesOfAllNonZeroArgOperations(schema: Schema): Set<Type> {
      val noArgsOperations = schema.operationsWithNoArgument()
      val eligibleFunctionsToSearch = schema
         .servicesAndOperations()
         .minus(noArgsOperations)

      return eligibleFunctionsToSearch.map { (_, operation) ->
         operation.returnType.collectionType ?: operation.returnType
      }.toSet()
   }


   private fun pathsForTargets(
      startingType: Type,
      schema: Schema,
      targetTypes: Set<Type>,
      graphBuilder: VyneGraphBuilder): Pair<Type, List<Pair<Type, List<SimplifiedPath>>>> {
      val defaultValue = defaultValueForType(startingType, schema)

      val fact = if (defaultValue is Map<*, *>)
         TypedObject.fromAttributes(startingType, defaultValue as Map<String, Any>, schema, false, DefinedInSchema)
      else
         TypedInstance.from(startingType, defaultValue, schema)
      val graph = graphBuilder.build(listOf(fact),
         emptySet(),
         emptyList(),
         emptySet()).graph

      val problem = GraphSearchProblem
         .startingFrom(providedInstance(fact))
         .`in`(graph)
         .takeCostsFromEdges()
         .build()
      val solutions = targetTypes.mapNotNull { targetType ->
         if (startingType == targetType || targetType.inheritsFrom(startingType)) {
            return@mapNotNull null
         }
         val solution = Hipster.createAStar(problem).search(type(targetType))
         if (solution.goalNode.state() == type(targetType)) {
            Pair(targetType, solution.goalNodes.map { it.simplifyPath() })
         } else {
            null
         }
      }
      return startingType to solutions
   }
   private fun accessibleFromThroughFunctionInvocations(schema: Schema,
                                                        fqn: String,
                                                        operationsSearchSpace: Set<Pair<Service, Operation>>):
      List<Algorithm<Operation, Type, WeightedNode<Operation, Type, Double>>.SearchResult> {
      val startingType = schema.type(fqn)
      val builder = HipsterGraphBuilder.create<Type, Operation>()
      val connections = operationsSearchSpace.flatMap { (_, operation) ->
         val argument = operation.parameters.first().type

         resolveSynonym(argument, schema).map { synonymType ->
            HipsterGraphBuilder.Connection(synonymType, operation.returnType, operation)
         } + HipsterGraphBuilder.Connection(argument, operation.returnType, operation)

      }
      val graph = builder.createDirectedGraph(connections)
      val problem = GraphSearchProblem
         .startingFrom(startingType)
         .`in`(graph)
         .takeCostsFromEdges()
         .build()

      val targetVertices = graph.vertices().filter { it != startingType }

      return targetVertices.mapNotNull { target ->
         val solution = Hipster.createAStar(problem).search(target)
         if (solution.goalNode.state() == target) {
            solution
         } else {
            null
         }
      }
   }


   private fun resolveSynonym(type: Type, schema: Schema): Set<Type> {
      return if (type.isEnum) {
         val underlyingEnumType = type.taxiType as EnumType
         underlyingEnumType.values.flatMap { enumValue ->
            enumValue.synonyms.map { synonym -> schema.type(synonym.synonymFullyQualifiedName()) }
         }.toSet()
      } else {
         setOf()
      }
   }

   /**
    * Naive implementation of 'default' value derivation for the given type.
    */
   private fun defaultValueForType(type: Type, schema: Schema): Any {
      if (type.attributes.isNotEmpty()) {
         return type.attributes.map { attribute ->
            val attrType = schema.type(attribute.value.type)
            attribute.key to defaultValueForType(attrType, schema)
         }.toMap()
      }

      if (type.isEnum) {
         val enumType = type.taxiType as EnumType
         return enumType.values.first().value
      }

      val value = type.taxiType.basePrimitive.let { primitiveType ->
         when (primitiveType) {
            PrimitiveType.STRING -> "default"
            PrimitiveType.INTEGER -> 0
            PrimitiveType.INSTANT -> Instant.parse("2020-05-14T22:00:00Z")
            PrimitiveType.DOUBLE -> 0.0
            PrimitiveType.DECIMAL -> 0.0
            PrimitiveType.TIME -> LocalTime.of(0, 0, 6)
            PrimitiveType.LOCAL_DATE -> LocalDate.now()
            PrimitiveType.BOOLEAN -> true
            PrimitiveType.DATE_TIME -> LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 6))
            else -> throw UnsupportedOperationException("$primitiveType not supported")
         }

      }

      return value ?: throw UnsupportedOperationException("Only primitive types are supported.")
   }
}


data class Dataset(
   val startType: QualifiedName,
   val exploredType: QualifiedName,
   val path: List<SimplifiedPath>
)

data class OperationQueryResult(val typeName: String , val results: List<OperationQueryResultItem>) {
   companion object {
      fun empty(typeName:String) = OperationQueryResult(typeName, emptyList())
   }

}
data class OperationQueryResultItem(
   val serviceName: QualifiedName,
   // Operation information may be null if the
   // consumption has been declared via lineage in a schema,
   // rather than by traversing inputs / outputs of operations
   val operationDisplayName: String?,
   val operationName: QualifiedName?,
   val role: OperationQueryResultItemRole
)

data class AnnotationSearchResult(
   val annotatedType: String,
   val containingType: String? = null,
   val fieldName: String? = null) {
   fun containingTypeAndFieldName() = if (containingType != null && fieldName != null) "$containingType:$fieldName" else null
}

enum class OperationQueryResultItemRole {
   /**
    * Consumes as an argument / input to an operation
    */
   Input,

   /**
    * Exposes an an output / return value from an operation
    */
   Output
}


private fun Annotatable.hasAnnotationContaining(searchTerm: String):Boolean {
   return this.annotations.any { it.qualifiedName.toLowerCase().contains(searchTerm.toLowerCase().removePrefix("@").removePrefix("#")) }
}
