package com.orbitalhq.schemaServer.core.repositories.lifecycle

import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import reactor.core.publisher.Flux

interface RepositorySpecLifecycleEventSource {
   val gitSpecAdded: Flux<GitSpecAddedEvent>
   val gitSpecRemoved: Flux<GitSpecRemovedEvent>
   val fileSpecAdded: Flux<FileSpecAddedEvent>
   val fileSpecRemoved: Flux<FileSpecRemovedEvent>
}

data class GitSpecRemovedEvent(val spec: GitProjectSpec)
data class GitSpecAddedEvent(
   val spec: GitProjectSpec,
   val config: WorkspaceGitProjectConfig
)
data class FileSpecRemovedEvent(val spec: FileProjectSpec)

data class FileSpecAddedEvent(
   val spec: FileProjectSpec,
   val config: WorkspaceFileProjectConfig
)
