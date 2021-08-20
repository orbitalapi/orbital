package io.vyne.pipelines.jet.pipelines

import io.vyne.models.TypedInstance
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.MessageContentProvider
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema

data class PipelineMessage(
   val inputType: QualifiedName,
   val outputType: QualifiedName,
   val messageProvider: MessageContentProvider
) {
   fun readAsTypedInstance(schema:Schema):TypedInstance {
      return messageProvider.readAsTypedInstance(ConsoleLogger, schema.type(inputType), schema)
   }
}
