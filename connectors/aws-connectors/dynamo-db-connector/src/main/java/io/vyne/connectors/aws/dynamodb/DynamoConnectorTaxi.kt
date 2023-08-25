package io.vyne.connectors.aws.dynamodb

import lang.taxi.types.Annotation

object DynamoConnectorTaxi {

    internal const val namespace = "io.vyne.aws.dynamo"
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
                const val NAME = "${namespace}.Table"
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
            const val NAME = "${namespace}.DynamoService"
        }
    }
}
