package com.orbitalhq.cockpit.core.query

import com.google.common.hash.Hashing
import com.orbitalhq.history.chart.QueryVisualizationBuilder
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.history.DiagramLink
import com.orbitalhq.query.history.DiagramNode
import com.orbitalhq.query.history.DiagramNodeMember
import com.orbitalhq.query.history.NodeId
import com.orbitalhq.query.history.QueryPlanDiagramData
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.FailedSearch
import com.orbitalhq.models.OperationResultDataSourceWrapper
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.models.ValueWithType
import com.orbitalhq.models.conditional.EvaluatedWhenCaseSelection
import com.orbitalhq.query.history.DiagramNodeKind
import com.orbitalhq.query.history.HandleKind
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.SchemaMember
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.abbreviate
import com.orbitalhq.utils.orElse
import mu.KotlinLogging

// Successor to LineageSankeyViewBuilder
class QueryPlanDiagramBuilder(private val schema: Schema) : QueryVisualizationBuilder<QueryPlanDiagramData> {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val nodes: MutableMap<String, DiagramNode> = mutableMapOf()
   private val links: MutableSet<DiagramLink> = mutableSetOf()
   private val operationResultsToNodeIds = mutableMapOf<String, String>()
   override fun append(instance: TypedInstance) {
      when (instance) {
         is TypedObject -> buildForObject(instance)
         is TypedValue -> buildForTypedValue(instance)
         else -> logger.debug { "No diagram build strategy for TypedInstance of type ${instance::class.simpleName}" }
      }
   }

   // These are methods for manually building a diagram (not a query plan)
   // for things like vSCode plugin
   fun addType(type: Type) {
      val typeNode = getOrCreateType(type)
      discoverLinksForType(type, typeNode)
   }

   // Manually adds a service. Used outside of query plans
   fun addService(service: Service) {
      val serviceNode = getOrCreateNode(service) { nodeId ->
         service.remoteOperations.map { operation ->
            DiagramNodeMember(
               "$nodeId::${operation.name}",
               operation.name,
               operation.returnType.paramaterizedName,
               emptySet()
            )
         }
      }
      discoverLinksForService(service, serviceNode)
   }

   /**
    * Discovers links involving the given type node.
    * This includes:
    * 1. Links from this type's fields to other types
    * 2. Links from other types' fields to this type
    * 3. Links between this type and service operations
    */
   private fun discoverLinksForType(type: Type, typeNode: DiagramNode) {
      // Discover links from this type's fields to other type nodes
      discoverTypeFieldLinks(type, typeNode)

      // Discover links from existing types that reference this type
      discoverInboundTypeFieldLinks(type, typeNode)

      // Discover links between this type and service operations
      discoverServiceOperationLinks(type, typeNode)
   }

   /**
    * Discovers links from this type's fields to other existing type nodes
    */
   private fun discoverTypeFieldLinks(type: Type, typeNode: DiagramNode) {
      type.attributes.forEach { (fieldName, field) ->
         val fieldType = field.resolveType(schema)
         if (!fieldType.isScalar) {
            val targetType = unwrapType(fieldType)
            val targetNodeId = DiagramNode.id(targetType)

            if (nodes.containsKey(targetNodeId)) {
               val memberHandleId = "${typeNode.id}::$fieldName"
               tryAddLink(typeNode.id, memberHandleId, targetNodeId, targetNodeId)
            }
         }
      }
   }

   /**
    * Discovers links from existing type nodes whose fields reference this type
    */
   private fun discoverInboundTypeFieldLinks(type: Type, typeNode: DiagramNode) {
      nodes.values
         .filter { it.kind == DiagramNodeKind.MODEL && it.id != typeNode.id }
         .mapNotNull { existingNode ->
            try {
               existingNode to schema.type(existingNode.qualifiedName?.fqn() ?: return@mapNotNull null)
            } catch (e: Exception) {
               null
            }
         }
         .forEach { (existingNode, existingType) ->
            existingType.attributes.forEach { (fieldName, field) ->
               val fieldType = field.resolveType(schema)
               if (!fieldType.isScalar && unwrapType(fieldType) == type) {
                  val memberHandleId = "${existingNode.id}::$fieldName"
                  tryAddLink(existingNode.id, memberHandleId, typeNode.id, typeNode.id)
               }
            }
         }
   }

   /**
    * Discovers links between a type and service operations
    */
   private fun discoverServiceOperationLinks(type: Type, typeNode: DiagramNode) {
      nodes.values
         .filter { it.kind == DiagramNodeKind.SERVICE }
         .mapNotNull { serviceNode ->
            try {
               if (serviceNode.qualifiedName == null) {
                  return@mapNotNull null
               } else {
                  serviceNode to schema.service(serviceNode.qualifiedName!!)
               }
            } catch (e: Exception) {
               null
            }
         }
         .forEach { (serviceNode, service) ->
            service.remoteOperations.forEach { operation ->
               val operationHandleId = "${serviceNode.id}::${operation.name}"

               // Link from operation to return type (RHS)
               if (unwrapType(operation.returnType) == type) {
                  tryAddLink(
                     serviceNode.id, operationHandleId,
                     typeNode.id, typeNode.id,
                     sourceHandleKind = HandleKind.RHS,
                     targetHandleKind = HandleKind.LHS
                  )
               }

               // Link from input types to operation (LHS)
               operation.parameters.forEach { param ->
                  if (unwrapType(param.type) == type) {
                     tryAddLink(
                        typeNode.id, typeNode.id,
                        serviceNode.id, operationHandleId,
                        sourceHandleKind = HandleKind.RHS,
                        targetHandleKind = HandleKind.LHS
                     )
                  }
               }
            }
         }
   }

   /**
    * Discovers links for a service node and its operations
    */
   private fun discoverLinksForService(service: Service, serviceNode: DiagramNode) {
      service.remoteOperations.forEach { operation ->
         val operationHandleId = "${serviceNode.id}::${operation.name}"

         // Link from operation to return type (RHS)
         val returnType = unwrapType(operation.returnType)
         val returnTypeId = DiagramNode.id(returnType)
         if (nodes.containsKey(returnTypeId)) {
            tryAddLink(
               serviceNode.id, operationHandleId,
               returnTypeId, returnTypeId,
               sourceHandleKind = HandleKind.RHS,
               targetHandleKind = HandleKind.LHS
            )
         }

         // Link from input types to operation (LHS)
         operation.parameters.forEach { param ->
            val paramType = unwrapType(param.type)
            val paramTypeId = DiagramNode.id(paramType)
            if (nodes.containsKey(paramTypeId)) {
               tryAddLink(
                  paramTypeId, paramTypeId,
                  serviceNode.id, operationHandleId,
                  sourceHandleKind = HandleKind.RHS,
                  targetHandleKind = HandleKind.LHS
               )
            }
         }
      }
   }

   /**
    * Unwraps collection and stream types to get the underlying type
    */
   private fun unwrapType(type: Type): Type {
      return when {
         type.isStream && type.typeParameters.isNotEmpty() -> unwrapType(type.typeParameters[0])
         type.isCollection && type.typeParameters.isNotEmpty() -> unwrapType(type.typeParameters[0])
         else -> type
      }
   }

   /**
    * Attempts to add a link, skipping if nodes don't exist or if it would be a self-reference.
    * Uses Set semantics to automatically avoid duplicate links.
    */
   private fun tryAddLink(
      sourceNodeId: String,
      sourceHandleId: String,
      targetNodeId: String,
      targetHandleId: String,
      sourceHandleKind: HandleKind = HandleKind.RHS,
      targetHandleKind: HandleKind = HandleKind.LHS
   ) {
      // Skip self-references
      if (sourceNodeId == targetNodeId && sourceHandleId == targetHandleId) {
         return
      }

      // Skip if nodes don't exist
      if (!(nodes.containsKey(sourceNodeId) && nodes.containsKey(targetNodeId))) {
         return
      }

      val link = DiagramLink(
         sourceNodeId, sourceHandleId,
         targetNodeId, targetHandleId,
         sourceHandleKind, targetHandleKind
      )
      links.add(link)
   }

   private fun buildForTypedValue(instance: TypedValue) {
      when {
         instance.type.isScalar -> {
            // For scalar values, we create a node for the type and link from the data source
            val typeNode = getOrCreateType(instance.type)
            val sourceNode = getOrCreateDataSource(instance.source, instance)
            if (sourceNode != null) {
               // Create link from source to the type node
               // For scalar types, link to the node itself rather than a specific member
               addLink(sourceNode.id, sourceNode.id, typeNode.id, typeNode.id)
            }
         }

         // instnace.value should never be a TypedObject
//         instance.value is TypedObject -> buildForObject(instance.value as TypedObject, emptyList())
         else -> {
            logger.warn { "Appending diagram data failed. Expected either a scalar value, or a TypedObject - but neither condition was true. ValueType = ${instance::class.simpleName}" }
         }
      }
   }

   private fun buildForObject(value: TypedObject, prefixes: List<String> = emptyList()) {
      val typeNode = getOrCreateType(value.type)

      value.map { (attributeName, instance) ->
         when {
            instance.type.isScalar -> {
               // Create or get the attribute member on the type node
               val member = typeNode.members.find { it.name == attributeName }
               if (member != null) {
                  val sourceNode =
                     getOrCreateDataSource(instance.source, instance, preferResponseObjectIfOperationResult = true)
                  if (sourceNode != null) {
                     val sourceHandleId = sourceNode.member(instance.typeName)?.handleId ?: sourceNode.id
                     // Create link from source node to this specific attribute member
                     addLink(sourceNode.id, sourceHandleId, typeNode.id, member.handleId)
                  }
               }
            }

            instance is TypedObject -> {
               buildForObject(instance, prefixes + attributeName)
            }

            instance is TypedCollection -> {
               instance.value
                  .filterIsInstance<TypedObject>()
                  .forEach { collectionMember -> buildForObject(collectionMember, prefixes) }
            }

            instance is TypedNull -> {
               // Do nothing for null values
            }

            else -> {
               logger.warn { "Appending diagram data failed. Expected either a scalar value, or a TypedObject - but neither condition was true. ValueType = ${instance::class.simpleName}" }
            }
         }
      }
   }

   private fun addLink(
      sourceNodeId: String,
      sourceHandleId: String,
      targetNodeId: String,
      targetHandleId: String,
      // Allows defining how this can be linked
      // Generally, in a query plan sources are always RHS ONLY (outbound) and
      // targets are LHS_ONLY (inbound)
      sourceHandleKind: HandleKind = HandleKind.RHS,
      targetHandleKind: HandleKind = HandleKind.LHS
   ) {
      if (sourceNodeId == targetNodeId && sourceHandleId == targetHandleId) {
         logger.debug { "Refusing to append a self-referencing link for $sourceHandleId" }
         return
      }
      if (!(nodes.containsKey(sourceNodeId) && nodes.containsKey(targetNodeId))) {
         error("Cannot add a link to a non-existent node")
      }

      val link =
         DiagramLink(sourceNodeId, sourceHandleId, targetNodeId, targetHandleId, sourceHandleKind, targetHandleKind)
      links.add(link)
   }

   private fun getOrCreateDataSource(
      dataSource: DataSource,
      value: ValueWithType,
      preferResponseObjectIfOperationResult: Boolean = false
   ): DiagramNode? {
      // Ignored data sources
      if (dataSource is FailedSearch || dataSource is UndefinedSource) {
         return null
      }

      return when (dataSource) {
         is Provided -> getOrCreateProvidedNode(value)
         is EvaluatedExpression -> getOrCreateExpressionNode(dataSource)
         is EvaluatedWhenCaseSelection -> getOrCreateWhenClauseNode(dataSource)
         is OperationResultDataSourceWrapper -> {
            getOrCreateDataSource(
               dataSource.operationResultReferenceSource,
               value,
               preferResponseObjectIfOperationResult
            )
         }

         is OperationResultReference -> {
            // Look up the operation node using the operation result ID mapping
            if (preferResponseObjectIfOperationResult) {
               val operation = schema.remoteOperation(dataSource.operationName).second
               val resultType = operation.returnType
               getOrCreateType(resultType)
            } else {
               val nodeId = operationResultsToNodeIds[dataSource.remoteCallId]
               if (nodeId != null) {
                  nodes[nodeId]
               } else {
                  logger.warn { "No operation node found for operation result ${dataSource.remoteCallId}" }
                  null
               }
            }
         }

         else -> {
            logger.debug { "No diagram node mapping for DataSource of type ${dataSource::class.simpleName}" }
            null
         }
      }
   }


   private fun getOrCreateProvidedNode(value: ValueWithType): DiagramNode {
      val valueOrNull = value.value?.toString()?.orElse("null")
      val typeName = value.typeName
      val id = "ProvidedInput/$typeName/$valueOrNull"
      return nodes.getOrPut(id) {
         DiagramNode(
            id = id,
            kind = DiagramNodeKind.CONSTANT,
            title = "$valueOrNull (${typeName.fqn().shortDisplayName})",
            qualifiedName = typeName,
         )
      }
   }

   private fun getOrCreateWhenClauseNode(dataSource: EvaluatedWhenCaseSelection): DiagramNode? {
      fun handleIdForCase(index: Int): String = "when-${dataSource.id}-case-$index"
      val nodeId = taxiToNodeId(dataSource.expressionTaxi)
      val expressionNode = nodes.getOrPut(nodeId) {

         // Building a when... block requires us to first declare the cases as members
         // then later we'll provide lineage mappings, as we need the nodes to exist
         // before we can build links
         val members = dataSource.evaluatedCases.mapIndexed { index, evaluatedCase ->
            val handleId = handleIdForCase(index)
            val displayedText = when (val evaluatedCaseDatasource = evaluatedCase.source) {
               is EvaluatedExpression -> evaluatedCaseDatasource.expressionTaxi.abbreviate()
               else -> evaluatedCaseDatasource.name
            }
            DiagramNodeMember(
               handleId = handleId,
               displayedText, evaluatedCase.typeName, handleId, HandleKind.LHS_ONLY
            )
         }
         DiagramNode(
            id = nodeId,
            kind = DiagramNodeKind.EXPRESSION,
            title = dataSource.expressionTaxi.abbreviate(),
            qualifiedName = null,
            members = members
         )
      }
      dataSource.evaluatedCases.mapIndexed { index, typedInstance ->
         val source = typedInstance.source
         if (source is EvaluatedExpression) {
            val expression = source
            val handleId = handleIdForCase(index)
            expression.inputs.forEach { input ->
               val inputSourceNode =
                  getOrCreateDataSource(input.source, input, preferResponseObjectIfOperationResult = true)
               if (inputSourceNode != null) {
                  val sourceHandleId = inputSourceNode.member(input.typeName)?.handleId ?: inputSourceNode.id
                  addLink(inputSourceNode.id, sourceHandleId, expressionNode.id, handleId)
               }
            }
         }
      }

      return expressionNode
   }

   /**
    * When using taxi expressions as data sources, we want to ensure
    * the expression only appears once in the diagram, so this returns
    * a consistent id for the taxi statement
    */
   private fun taxiToNodeId(taxi: String): String {
      val withoutWhitespace = taxi.replace("\\s+".toRegex(), "")
      val hash = Hashing.murmur3_128()
         .hashBytes(withoutWhitespace.toByteArray())
         .toString()
      return withoutWhitespace.take(12) + hash
   }

   private fun getOrCreateExpressionNode(expression: EvaluatedExpression): DiagramNode {
      val nodeId = taxiToNodeId(expression.expressionTaxi)
      val expressionNode = nodes.getOrPut(nodeId) {
         DiagramNode(
            nodeId,
            DiagramNodeKind.EXPRESSION,
            expression.expressionTaxi.abbreviate(),
            qualifiedName = null
         )
      }

      // Process the inputs to the expression and create links
      expression.inputs.forEach { input ->
         val inputSourceNode = getOrCreateDataSource(input.source, input, preferResponseObjectIfOperationResult = true)
         if (inputSourceNode != null) {
            val sourceHandleId = inputSourceNode.member(input.typeName)?.handleId ?: inputSourceNode.id
            addLink(inputSourceNode.id, sourceHandleId, expressionNode.id, expressionNode.id)
         }
      }

      return expressionNode
   }

   fun getOrCreateType(type: Type): DiagramNode {
      return when {
         type.isStream -> getOrCreateType(type.typeParameters[0])
         type.isCollection -> getOrCreateType(type.typeParameters[0])
         else -> getOrCreateNode(type) { nodeId ->
            type.attributes.map { (name, attribute) ->
               DiagramNodeMember("$nodeId::$name", name, attribute.type.shortDisplayName, attribute.type)
            }
         }
      }
   }


   private fun getOrCreateNode(
      member: SchemaMember,
      nodeHandles: Set<HandleKind> = HandleKind.UNDEFINED,
      id: NodeId = DiagramNode.id(member),
      memberBuilder: (NodeId) -> List<DiagramNodeMember>
   ): DiagramNode {
      return nodes.getOrPut(id) {
         val members = memberBuilder(id)
         DiagramNode.forSchemaMember(member, nodeHandles, members)
      }
   }

   override fun captureCachedOperationWithUniquePathObserved(
      operationResultReference: OperationResultReference,
      queryId: String
   ) {
      val (_, remoteOperation) = schema.remoteOperation(operationResultReference.operationName)
      observeOperation(remoteOperation, operationResultReference.inputs, operationResultReference.id)
   }

   override fun captureOperationResult(operationResult: OperationResult) {
      val (_, operation) = schema.remoteOperation(operationResult.remoteCall.operationQualifiedName)
      observeOperation(operation, operationResult.inputs, operationResult.id)
   }

   private fun observeOperation(
      operation: RemoteOperation,
      inputs: List<OperationResult.OperationParam>,
      dataSourceId: String
   ) {
      // nodeHandles: An operationResult has a LHS: operation -> operationResult(LHS)
      // An operationResult has a RHS operationResult(RHS) -> [somewhere where it's used as an input)
      val operationNode = getOrCreateNode(operation, nodeHandles = HandleKind.LHS_AND_RHS) { nodeId ->
         operation.parameters.mapIndexed { index, parameter ->
            val name = parameter.name ?: "p$index"
            // memberHandles:
            // The members of an operationResult can ONLY be linked from the RHS
            // eg: member(RHS) -> [somewhere it's used as an input]
            DiagramNodeMember(
               "$nodeId::$name",
               name,
               parameter.type.name.shortDisplayName,
               parameter,
               supportedHandles = HandleKind.RHS_ONLY
            )
         }
      }

      operationResultsToNodeIds[dataSourceId] = operationNode.id

      // Build connections for each of the inputs to the operation.
      inputs.forEach { operationParam ->
         when (val value = operationParam.value) {
            is TypeNamedInstance -> {
               val sourceNode = if (isMixedSourcesAndShouldIntrospect(value)) {
                  // For request objects, we create a new node with all properties linked
                  appendParameterRequestObject(value)
               } else {
                  // For scalar inputs or single-source objects, get the source node directly
                  lookupSourceNode(value.dataSourceId, value, preferResponseObjectIfOperationResult = true)
               }

               if (sourceNode != null) {
                  // Find the parameter member on the operation node
                  val paramMember = operationNode.member(operationParam.parameterName)
                  val sourceHandleId = sourceNode.member(value.typeName)?.handleId ?: sourceNode.id
                  if (paramMember != null) {
                     addLink(
                        sourceNode.id,
                        sourceHandleId,
                        operationNode.id,
                        paramMember.handleId,
                     )
                  } else {
                     // Fallback: link to the operation node itself
                     addLink(
                        sourceNode.id,
                        sourceNode.id,
                        operationNode.id,
                        operationNode.id,
                     )
                  }
               } else if (value.dataSourceId != null && value.dataSourceId != UndefinedSource.id) {
                  logger.warn { "Received dataSourceId ${value.dataSourceId} for input parameter ${operationParam.parameterName} but that has not yet been mapped" }
               }
            }

            null -> logger.debug { "Not recording null value as input to param ${operationParam.parameterName}" }
            else -> logger.warn { "Unhandled type of operationParam value: ${value!!::class.simpleName}" }
         }
      }

      // Also, append the result object
      // TODO : Perhaps this needs to be unique, linked to the data source -- the operationResult.id
      val responseTypeNode = getOrCreateType(
         type = operation.returnType,
      ) // members from an Api response can only be linked outward)
      addLink(operationNode.id, operationNode.id, responseTypeNode.id, responseTypeNode.id)
   }


   private fun isMixedSourcesAndShouldIntrospect(typeNamedInstance: TypeNamedInstance): Boolean {
      return if (typeNamedInstance.value !is Map<*, *>) {
         false
      } else {
         (typeNamedInstance.value as Map<*, *>).values.any { it is TypeNamedInstance }
      }
   }

   private fun appendParameterRequestObject(
      value: TypeNamedInstance,
   ): DiagramNode {
      val type = schema.type(value.typeName)
      val requestParamNode = getOrCreateNode(type) { nodeId ->
         type.attributes.map { (name, attribute) ->
            DiagramNodeMember("$nodeId::$name", name, attribute.type.shortDisplayName, setOf(attribute.type, name))
         }
      }

      // Process the request object's properties
      val requestParam = value.value as Map<String, *>
      requestParam.values
         .filterIsInstance<TypeNamedInstance>()
         .forEach { paramValue ->
            val sourceNode =
               lookupSourceNode(paramValue.dataSourceId, value, preferResponseObjectIfOperationResult = true)
            if (sourceNode != null) {
               val targetHandleId = requestParamNode.member(paramValue.typeName)?.handleId ?: requestParamNode.id
               val sourceHandleId = sourceNode.member(paramValue.typeName)?.handleId ?: sourceNode.id
               addLink(sourceNode.id, sourceHandleId, requestParamNode.id, targetHandleId)
            } else {
               logger.warn { "Unable to find source node with id ${paramValue.dataSourceId}" }
            }
         }

      return requestParamNode
   }

   private fun lookupSourceNode(
      dataSourceId: String?,
      value: TypeNamedInstance,
      preferResponseObjectIfOperationResult: Boolean = false
   ): DiagramNode? {
      return when (dataSourceId) {
         Provided.id -> getOrCreateProvidedNode(value)
         null -> null
         else -> {
            val nodeId = operationResultsToNodeIds[dataSourceId]
            if (nodeId == null) {
               logger.warn { "Trying to lookup node from dataSourceId $dataSourceId failed - no corresponding dataSource found with datasourceId" }
               null
            } else {
               nodes[nodeId]?.let { node ->
                  if (node.kind == DiagramNodeKind.OPERATION && preferResponseObjectIfOperationResult && node.qualifiedName != null) {
                     val (_, operation) = schema.remoteOperation(node.qualifiedName!!.fqn())
                     getOrCreateType(operation.returnType)
                  } else {
                     node
                  }
               }

            }
         }
      }
   }

   override fun build(queryId: String): QueryPlanDiagramData {
      // Organize links by node
      val nodesWithOrganizedLinks = organizeLinksIntoNodes()

      return QueryPlanDiagramData(
         queryId,
         nodesWithOrganizedLinks,
         links.toList()
      )
   }

   private fun organizeLinksIntoNodes(): List<DiagramNode> {
      // Build maps to organize links by node
      val inboundHeaderLinksMap = mutableMapOf<String, MutableList<DiagramLink>>()
      val outboundHeaderLinksMap = mutableMapOf<String, MutableList<DiagramLink>>()
      val memberLinksMap = mutableMapOf<String, MutableMap<String, MutableList<DiagramLink>>>()

      // Initialize maps for all nodes
      nodes.keys.forEach { nodeId ->
         inboundHeaderLinksMap[nodeId] = mutableListOf()
         outboundHeaderLinksMap[nodeId] = mutableListOf()
         memberLinksMap[nodeId] = mutableMapOf()
      }

      // Categorize each link
      links.forEach { link ->
         // Categorize source side (always outbound from source)
         if (link.sourceHandleId == link.sourceId) {
            // Header-level link from source
            outboundHeaderLinksMap[link.sourceId]?.add(link)
         } else {
            // Member-level link from source
            memberLinksMap[link.sourceId]
               ?.getOrPut(link.sourceHandleId) { mutableListOf() }
               ?.add(link)
         }

         // Categorize target side (always inbound to target)
         if (link.targetHandleId == link.targetId) {
            // Header-level link to target
            inboundHeaderLinksMap[link.targetId]?.add(link)
         } else {
            // Member-level link to target
            memberLinksMap[link.targetId]
               ?.getOrPut(link.targetHandleId) { mutableListOf() }
               ?.add(link)
         }
      }

      // Create updated nodes with organized links
      return nodes.map { (nodeId, node) ->
         node.copy(
            inboundHeaderLinks = inboundHeaderLinksMap[nodeId] ?: emptyList(),
            outboundHeaderLinks = outboundHeaderLinksMap[nodeId] ?: emptyList(),
            memberLinks = memberLinksMap[nodeId]?.mapValues { it.value.toList() } ?: emptyMap()
         )
      }
   }
}
