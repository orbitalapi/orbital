package com.orbitalhq.pipelines.jet.api.streams

import com.orbitalhq.schemas.QualifiedName

object StreamUtils {
    /**
     * pipeline name is referred from multiple places - stream engine and the Telemetry service.
     * We used to have a flow where the stream engine creates the pipeline name by concatenating
     * Qualified name and schema hash but the Telemetry service referring the same pipeline by using the QualifiedName
     * only. To avoid such mismatches, we have this function that generates the pipeline name from the given query
     * Qualified Name.
     */
    fun toPipelineName(name: QualifiedName) = name.longDisplayName
}