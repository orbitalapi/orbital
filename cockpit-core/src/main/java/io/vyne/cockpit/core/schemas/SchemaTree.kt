package io.vyne.cockpit.core.schemas

import io.vyne.schemas.*
import lang.taxi.types.Arrays
import lang.taxi.types.TypeKind

/**
 * service for the UI, which allows us to explore
 * the schema as a tree.
 *
 * If no node is provided as the starting point, then
 * returns services as a top-level.
 *
 *
 */
object SchemaTreeUtils {
   fun getRootNodes(schema: Schema): List<SchemaTreeNode> {
      return schema.services.map { SchemaTreeNode.forService(it) }
         .sortedBy { it.element.shortDisplayName }
   }

   fun getChildNodes(node: QualifiedName, schema: Schema): List<SchemaTreeNode> {
      return when (val element = schema.getMember(node)) {
         is Service -> {
            if (element.serviceKind == ServiceKind.Database) {
               element.tableOperations.map { tableOperation ->
                  SchemaTreeNode.forTableOperation(tableOperation)
               }
            }
            element.remoteOperations.map { remoteOperation ->
               SchemaTreeNode.forOperation(remoteOperation)
            }
         }

         is QueryOperation -> {
            val operationReturnType = Arrays.unwrapPossibleArrayType(element.returnType.taxiType)
            listOf(SchemaTreeNode.forType(schema.type(operationReturnType.qualifiedName)))
         }
         is StreamOperation -> {
            val operationReturnType = Arrays.unwrapPossibleArrayType(element.returnType.taxiType)
            listOf(SchemaTreeNode.forType(schema.type(operationReturnType.qualifiedName)))
         }

         is Operation -> {
            val operationReturnType = Arrays.unwrapPossibleArrayType(element.returnType.taxiType)
            listOf(SchemaTreeNode.forType(schema.type(operationReturnType.qualifiedName)))
         }

         is Type -> {
            element.attributes.map { (fieldName, field) ->
               SchemaTreeNode.forField(fieldName, field, schema)
            }
         }

         else -> error("Unexpected schema member kind: ${element::class.simpleName}")
      }
   }
}

data class SchemaTreeNode(
   val element: QualifiedName,
   val schemaMemberKind: SchemaMemberKind,
   val hasChildren: Boolean,
   val typeDoc: String?,
   val serviceKind: ServiceKind? = null,
   val typeKind: TypeKind? = null,
   val operationKind: OperationKind? = null,
   val fieldName: String? = null,
   val primitiveType: QualifiedName? = null
) {
   companion object {
      fun forField(fieldName: String, field: Field, schema: Schema): SchemaTreeNode {
         val fieldType = schema.type(field.type)
         return SchemaTreeNode(
            field.type,
            SchemaMemberKind.FIELD,
            hasChildren = !fieldType.isScalar,
            typeKind = fieldType.taxiType.typeKind,
            typeDoc = (field.typeDoc + "\n" + fieldType.typeDoc).trim(),
            fieldName = fieldName,
            primitiveType = fieldType.basePrimitiveTypeName
         )
      }

      fun forType(type: Type): SchemaTreeNode {
         return SchemaTreeNode(
            type.name,
            SchemaMemberKind.TYPE,
            hasChildren = !type.isScalar,
            typeDoc = type.typeDoc,
            typeKind = type.taxiType.typeKind,
            primitiveType = type.basePrimitiveTypeName

         )
      }

      fun forOperation(operation: RemoteOperation): SchemaTreeNode {
         return SchemaTreeNode(
            operation.qualifiedName,
            SchemaMemberKind.OPERATION,
            typeDoc = operation.typeDoc,
            hasChildren = true,
            operationKind = operation.operationKind
         )
      }

      fun forService(service: Service): SchemaTreeNode {
         return SchemaTreeNode(
            service.name,
            service.schemaMemberKind,
            typeDoc = service.typeDoc,
            hasChildren = service.remoteOperations.isNotEmpty(),
            serviceKind = service.serviceKind
         )
      }

      fun forTableOperation(tableOperation: TableOperation): SchemaTreeNode {
         return SchemaTreeNode(
            tableOperation.returnType.name,
            SchemaMemberKind.TYPE,
            hasChildren = true,
            tableOperation.typeDoc,
            typeKind = TypeKind.Model
         )
      }
   }
}
