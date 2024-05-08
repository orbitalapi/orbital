package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig
import reactor.core.publisher.Flux

interface RepositorySpecLifecycleEventSource {
   val gitSpecAdded: Flux<GitSpecAddedEvent>
   val gitSpecRemoved: Flux<GitSpecRemovedEvent>
   val fileSpecAdded: Flux<FileSpecAddedEvent>
   val fileSpecRemoved: Flux<FileSpecRemovedEvent>
}

data class GitSpecRemovedEvent(val spec: GitProjectStoreSpec)
data class GitSpecAddedEvent(
    val spec: GitProjectStoreSpec,
    val config: GitSchemaRepositoryConfig
)
data class FileSpecRemovedEvent(val spec: FileSystemPackageSpec)

data class FileSpecAddedEvent(
   val spec: FileSystemPackageSpec,
   val config: FileSystemSchemaRepositoryConfig
)
