package com.orbitalhq.utils.files

import com.orbitalhq.utils.files.FileSystemChangeEvent.FileSystemChangeEventType
import mu.KotlinLogging
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.HiddenFileFilter
import org.apache.commons.io.filefilter.IOFileFilter
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.io.File
import java.nio.file.Path
import java.time.Duration

class ReactivePollingFileSystemMonitor(
   private val rootPath: Path,
   val pollFrequency: Duration
) : ReactiveFileSystemMonitor {

   private val sink = Sinks.many().replay().latest<List<FileSystemChangeEvent>>()
   private val suspendedEvents = SuspendableEventPublisher.forFileSystemChangeEvents(sink)

   private val logger = KotlinLogging.logger {}

   private val monitor: FileAlterationMonitor
   private val observer: FileAlterationObserver

   init {

      logger.info("Creating a file poller at ${rootPath.toFile().canonicalPath} polling $pollFrequency")
      val directories: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.directoryFileFilter(),
         HiddenFileFilter.VISIBLE
      )
      val taxiFiles: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.fileFileFilter(),
         FileFilterUtils.suffixFileFilter(".taxi")
      )
      val filter: IOFileFilter = FileFilterUtils.or(directories, taxiFiles)
         .or(FileFilterUtils.suffixFileFilter(".conf"))
      observer = FileAlterationObserver(rootPath.toFile(), filter).apply {
         addListener(object : FileAlterationListener {
            override fun onStart(observer: FileAlterationObserver) {
               logger.debug("File poll starting path => ${rootPath.toFile().canonicalPath}")
            }

            override fun onDirectoryCreate(directory: File) {
               //no need to emit for this as we're getting file events insted.
               //emitChangeEvent(directory, FileSystemChangeEventType.DirectoryCreated)
            }

            override fun onDirectoryChange(directory: File) {
               //no need to emit for this as we're getting file events insted.
               //emitChangeEvent(directory, FileSystemChangeEventType.DirectoryChanged)
            }

            override fun onDirectoryDelete(directory: File) {
               //no need to emit for this as we're getting file events insted.
               // emitChangeEvent(directory, FileSystemChangeEventType.DirectoryDeleted)
            }

            override fun onFileCreate(file: File) {
               emitChangeEvent(file, FileSystemChangeEventType.FileCreated)
            }

            override fun onFileChange(file: File) {
               emitChangeEvent(file, FileSystemChangeEventType.FileChanged)
            }

            override fun onFileDelete(file: File) {
               emitChangeEvent(file, FileSystemChangeEventType.FileDeleted)
            }

            override fun onStop(observer: FileAlterationObserver) {
               logger.debug("File poll completed for path => ${rootPath.toFile().canonicalPath}")
            }

         })
      }
      monitor = FileAlterationMonitor(pollFrequency.toMillis())
      monitor.addObserver(observer)
   }

   private fun emitChangeEvent(file: File, type: FileSystemChangeEventType) {
      logger.info { "File changed: $type at ${file.canonicalPath}" }
      suspendedEvents.publish(listOf(FileSystemChangeEvent(file.toPath(), type)))
   }

   override fun startWatching(): Flux<List<FileSystemChangeEvent>> {
      monitor.start()
      return sink.asFlux()
   }

   override fun suspend() {
      logger.debug { "Suspending event publication for  ${rootPath.toFile().canonicalPath}" }
      suspendedEvents.suspend()
   }

   override fun resume() {
      logger.debug { "Resuming event publication for  ${rootPath.toFile().canonicalPath}" }
      suspendedEvents.resume()
   }

   override fun stop() {
      logger.debug { "Stopping event publication for  ${rootPath.toFile().canonicalPath}" }
      sink.tryEmitComplete()
      monitor.removeObserver(observer)
      monitor.stop()
   }

   fun pollNow() {
      observer.checkAndNotify()
   }
}
