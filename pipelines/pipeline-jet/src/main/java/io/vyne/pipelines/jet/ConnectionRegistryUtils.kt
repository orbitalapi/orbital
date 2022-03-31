package io.vyne.pipelines.jet

import io.vyne.connectors.registry.ConnectionRegistry
import io.vyne.connectors.registry.ConnectorConfiguration

fun <T : ConnectorConfiguration> ConnectionRegistry<T>.connectionOrError(
   pipelineId:String,
   connectionName:String
):T {
   if (!this.hasConnection(connectionName)) {
      throw BadRequestException("Pipeline $pipelineId defines an input from non-existent connection $connectionName")
   }
   return this.getConnection(connectionName)
}
