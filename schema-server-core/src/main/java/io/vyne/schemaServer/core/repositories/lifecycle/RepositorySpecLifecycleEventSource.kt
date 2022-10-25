package io.vyne.schemaServer.core.repositories.lifecycle

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
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
