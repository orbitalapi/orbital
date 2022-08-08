package io.vyne.schemaServer.core.file.packages

import reactor.core.publisher.Flux
import java.nio.file.Path

interface ReactiveFileSystemMonitor {
   fun startWatching(): Flux<List<FileSystemChangeEvent>>
   // Don't need to stop, just stop when all the subscribers to start() have gone away.
}

data class FileSystemChangeEvent(
   val path: Path,
   val eventType: FileSystemChangeEventType
) {
   enum class FileSystemChangeEventType {
      DirectoryCreated,
      DirectoryChanged,
      DirectoryDeleted,
      FileCreated,
      FileChanged,
      FileDeleted,
      Unspecified
   }
}
