package com.orbitalhq.utils.files

import reactor.core.publisher.Flux

/**
 * A file system monitor that does nothing.
 */
object NoOpFileSystemMonitor : ReactiveFileSystemMonitor {
   override fun startWatching(): Flux<List<FileSystemChangeEvent>> {
      return Flux.empty()
   }

}
