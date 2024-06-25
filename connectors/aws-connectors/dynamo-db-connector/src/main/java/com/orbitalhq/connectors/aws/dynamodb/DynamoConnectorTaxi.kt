package com.orbitalhq.connectors.aws.dynamodb

import com.orbitalhq.VyneTypes
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connections.ConnectionUsageRegistration
import com.orbitalhq.schemas.fqn
import lang.taxi.types.Annotation

object DynamoConnectorTaxi {
   fun registerConnectorUsage() {
      ConnectionUsageMetadataRegistry.register(
         ConnectionUsageRegistration(Annotations.Table.NAME.fqn(), Annotations.Table::connectionName.name)
      )
   }

    internal val namespace = "${VyneTypes.NAMESPACE}.aws.dynamo"
    val schema = """
        namespace $namespace {
            type ConnectionName inherits String
            annotation DynamoService {
            }

            annotation Table {
                connectionName : ConnectionName
                tableName : TableName inherits String
            }
        }
    """.trimIndent()

    object Annotations {
        data class Table(val connectionName: String, val tableName: String) {
            companion object {
                val NAME = "${namespace}.Table"
                fun from(annotation: Annotation): Table {
                    require(annotation.qualifiedName == NAME) { "Annotation name should be $NAME" }
                    return Table(
                        connectionName = annotation.parameters["connectionName"] as String,
                        tableName = annotation.parameters["tableName"] as String
                    )
                }
            }
        }

        object DynamoService {
            val NAME = "${namespace}.DynamoService"
        }
    }
}
