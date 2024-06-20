package com.orbitalhq.pipelines.jet.pipelines

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema

data class PipelineMessage(
   val inputType: QualifiedName,
   val outputType: QualifiedName,
   val messageProvider: MessageContentProvider
) {
   fun readAsTypedInstance(schema:Schema):TypedInstance {
      return messageProvider.readAsTypedInstance(schema.type(inputType), schema)
   }
}
