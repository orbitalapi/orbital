package com.orbitalhq.schemaServer.core.file

import com.orbitalhq.SourcePackage

data class SourcesChangedMessage(val packages: List<SourcePackage>)
