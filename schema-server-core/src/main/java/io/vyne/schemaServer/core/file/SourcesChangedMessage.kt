package io.vyne.schemaServer.core.file

import io.vyne.SourcePackage

data class SourcesChangedMessage(val packages: List<SourcePackage>)
