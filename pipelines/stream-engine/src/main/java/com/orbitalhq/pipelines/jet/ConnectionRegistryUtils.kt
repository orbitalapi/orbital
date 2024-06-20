package com.orbitalhq.pipelines.jet

import com.orbitalhq.connectors.registry.ConnectionRegistry
import com.orbitalhq.connectors.registry.ConnectorConfiguration

fun <T : ConnectorConfiguration> ConnectionRegistry<T>.connectionOrError(
   pipelineId:String,
   connectionName:String
):T {
   if (!this.hasConnection(connectionName)) {
      throw BadRequestException("Pipeline $pipelineId defines an input from non-existent connection $connectionName")
   }
   return this.getConnection(connectionName)
}
