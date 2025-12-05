package com.orbitalhq.query.history

import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.SchemaMember
import com.orbitalhq.schemas.SchemaMemberKind
import com.orbitalhq.schemas.StreamOperation
import com.orbitalhq.schemas.TableOperation
import com.orbitalhq.schemas.Type
import lang.taxi.services.Operation


data class QueryPlanDiagramData(
   val queryId: String,
   val nodes: List<DiagramNode>,
   val links: List<DiagramLink>
) {
   companion object {
      fun empty(queryId: String) = QueryPlanDiagramData(queryId, emptyList(), emptyList())
      val EMPTY = empty("")

   }
}

data class DiagramNodeMember(
   val handleId: String,
   val name: String,
   val typeName: String,
   // Use this to look up the member using the source.
   // Eg: A Field instance, or OperationParameter etc
   private val matchOn: Set<Any>,
   // undefined initially, populated once the chart data is built
   val supportedHandles: Set<HandleKind> = HandleKind.UNDEFINED
) {
   constructor(
      handleId: String,
      name: String,
      typeName: String,
      matchOn: Any,
      supportedHandles: Set<HandleKind> = HandleKind.UNDEFINED
   ) : this(handleId, name, typeName, setOf(matchOn), supportedHandles)

   private val matchCandidates: List<Any> = matchOn.flatMap { candidate ->
      when (candidate) {
         is Field -> matchesForQualifiedName(candidate.type)
         is String -> listOf(candidate)
         is Type -> matchesForType(candidate)
         is QualifiedName -> listOf(candidate, candidate.parameterizedName)
         is Parameter -> matchesForType(candidate.type) + candidate.name
         else -> emptyList()
      }.filterNotNull()
   }

   fun matches(other: Any) = matchCandidates.contains(other)

   companion object {
      private fun matchesForQualifiedName(name: QualifiedName): List<Any> {
         return listOf(name.parameterizedName, name)
      }

      private fun matchesForType(type: Type): List<Any> {
         return listOf(type) + matchesForQualifiedName(type.name)
      }
   }
}

enum class HandleKind {
   LHS,
   RHS,
   FLOATING;

   companion object {
      // Synonyms, which can make things more readable
      val INPUT = HandleKind.LHS
      val OUTPUT = HandleKind.RHS

      val UNDEFINED = setOf<HandleKind>()

      val LHS_AND_RHS: Set<HandleKind> = setOf(LHS, RHS)
      val RHS_ONLY: Set<HandleKind> = setOf(RHS)
      val LHS_ONLY: Set<HandleKind> = setOf(LHS)
      val INPUT_AND_OUTPUT: Set<HandleKind> = setOf(INPUT, OUTPUT)
      val INPUT_ONLY: Set<HandleKind> = setOf(INPUT)
      val OUTPUT_ONLY: Set<HandleKind> = setOf(OUTPUT)
   }
}

fun Set<HandleKind>.isUndefined(): Boolean = this === HandleKind.UNDEFINED

enum class DiagramNodeKind(
   val label: String,
   // Should be a tabler.io icon name,
   // or something specifically mapped in node-icon-mapping.ts
   val defaultIcon: String? = null
) {
   SERVICE("Service"),
   SCALAR_TYPE("Type", "code-variable"),
   REQUEST_MODEL("Request", "arrow-right-to-arc"),
   RESPONSE_MODEL("Response", "arrow-left-from-arc"),
   REQUEST_RESPONSE_MODEL("Request/Response model", "arrows-exchange"),
   MODEL("Model", "blocks"),
   QUERY("Query", "help-hexagon"),
   OPERATION("Operation", "switch-horizontal"),
   CONSTANT("Constant", "square-letter-c"),
   EXPRESSION("Expression", "file-lambda"),



   // More specific operation kinds
   KAFKA_TOPIC("Kafka topic", "Kafka"),
   API_CALL("Api call", "switch-horizontal"),
   DB_QUERY("SQL query", "file-type-sql"),
   DB_TABLE("Db table", "table" ),
   MONGO_DOCUMENT("Mongo document", "brand-mongodb"),
   MONGO_QUERY("Mongo query", "brand-mongodb");


   companion object {
      fun forSchemaMember(member: SchemaMember): DiagramNodeKind {
         return when (member.schemaMemberKind) {
            SchemaMemberKind.QUERY -> QUERY
            SchemaMemberKind.SERVICE -> SERVICE
            SchemaMemberKind.OPERATION -> {
               when (member) {
                   is StreamOperation -> {
                      when {
                         member.hasMetadata("com.orbitalhq.kafka.KafkaOperation") -> DiagramNodeKind.KAFKA_TOPIC
                         else -> OPERATION
                      }
                   }

                  is TableOperation -> {
                     val returnType = member.returnType.collectionType ?: member.returnType
                     when {
                        returnType.hasMetadata(" com.orbitalhq.jdbc.Table") -> DB_TABLE
                        returnType.hasMetadata("com.orbitalhq.mongo.Collection") -> MONGO_DOCUMENT
                        else -> OPERATION
                     }
                  }

                  is com.orbitalhq.schemas.Operation -> {
                     when {
                        member.hasMetadata("taxi.http.HttpOperation") -> API_CALL
                        else -> OPERATION
                     }
                  }

                  else -> OPERATION
               }
            }

            SchemaMemberKind.TYPE -> {
               val type = member as Type
               when {
                  member.isClosed && !member.isParameterType -> RESPONSE_MODEL
                  !member.isClosed && member.isParameterType -> REQUEST_MODEL
                  member.isClosed && member.isParameterType -> REQUEST_RESPONSE_MODEL
                  member.isScalar -> SCALAR_TYPE
                  else -> MODEL
               }
            }

            SchemaMemberKind.FIELD -> SCALAR_TYPE
         }
      }
   }
}

data class DiagramNode(
   val id: String,
   val kind: DiagramNodeKind,
   val title: String,
   val icon: String? = kind.defaultIcon,
   val qualifiedName: String?,
   val members: List<DiagramNodeMember> = emptyList(),
   val supportedHandles: Set<HandleKind> = HandleKind.UNDEFINED, // Undefined initially
   val badgeLabel: String = kind.label,

   // Helpers for the UI
   val inboundHeaderLinks: List<DiagramLink> = emptyList(),
   val outboundHeaderLinks: List<DiagramLink> = emptyList(),
   val memberLinks: Map<String, List<DiagramLink>> = emptyMap()
) {
   /**
    * Will match on most things here - parameter, qualified name, type
    */
   fun member(matchOn: Any): DiagramNodeMember? {
      return members.firstOrNull { it.matches(matchOn) }
   }

   companion object {
      fun id(member: SchemaMember): NodeId = member.qualifiedName.parameterizedName
      fun forSchemaMember(
         member: SchemaMember,
         nodeHandles: Set<HandleKind>,
         members: List<DiagramNodeMember>
      ): DiagramNode {
         val title = if (member is RemoteOperation) {
            OperationNames.displayNameFromOperationName(member.qualifiedName)
         } else {
            member.qualifiedName.shortDisplayName
         }
         val kind = DiagramNodeKind.forSchemaMember(member)
         return DiagramNode(
            id = id(member),
            kind = kind,
            title = title,
            icon = kind.defaultIcon,
            qualifiedName = member.qualifiedName.parameterizedName,
            members = members,
            supportedHandles = nodeHandles
         )
      }
   }

}

data class DiagramLink(
   val sourceId: String,
   val sourceHandleId: String,
   val targetId: String,
   val targetHandleId: String,
   val sourceHandleKind: HandleKind,
   val targetHandleKind: HandleKind
)

typealias NodeId = String
