package io.vyne.queryService.schemas.editor.generator

import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.queryService.schemas.editor.EditedSchema
import io.vyne.schemas.Operation
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.generators.NamingUtils.replaceIllegalCharacters
import lang.taxi.generators.NamingUtils.toCapitalizedWords
import lang.taxi.types.StreamType
