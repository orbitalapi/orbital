package io.vyne.connectors.aws.dynamodb

import lang.taxi.types.Type

fun getDynamoTableFromType(type: Type): DynamoConnectorTaxi.Annotations.Table {
    val annotation =
        type.annotations.firstOrNull { it.qualifiedName == DynamoConnectorTaxi.Annotations.Table.NAME }
            ?: error("Type ${type.qualifiedName} does not declare a ${DynamoConnectorTaxi.Annotations.Table.NAME} annotation")

    return DynamoConnectorTaxi.Annotations.Table.from(annotation)
}
