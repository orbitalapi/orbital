package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig
import reactor.core.publisher.Flux

interface RepositorySpecLifecycleEventSource {
   val gitSpecAdded: Flux<GitSpecAddedEvent>
   val fileSpecAdded: Flux<FileSpecAddedEvent>
}

data class GitSpecAddedEvent(
   val spec: GitRepositoryConfig,
   val config: GitSchemaRepositoryConfig
)

data class FileSpecAddedEvent(
   val spec: FileSystemPackageSpec,
   val config: FileSystemSchemaRepositoryConfig
)
