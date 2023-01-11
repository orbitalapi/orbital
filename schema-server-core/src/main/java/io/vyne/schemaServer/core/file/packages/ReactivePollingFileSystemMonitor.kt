package io.vyne.schemaServer.core.file.packages

import io.vyne.schemaServer.core.file.packages.FileSystemChangeEvent.FileSystemChangeEventType
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
   private val pollFrequency: Duration
) : ReactiveFileSystemMonitor {

   private val sink = Sinks.many().replay().latest<List<FileSystemChangeEvent>>()

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
      observer = FileAlterationObserver(rootPath.toFile(), filter).apply {
         addListener(object : FileAlterationListener {
            override fun onStart(observer: FileAlterationObserver) {
               logger.debug("File poll starting")
            }

            override fun onDirectoryCreate(directory: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.DirectoryCreated)
            }

            override fun onDirectoryChange(directory: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.DirectoryChanged)
            }

            override fun onDirectoryDelete(directory: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.DirectoryDeleted)
            }

            override fun onFileCreate(file: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.FileCreated)
            }

            override fun onFileChange(file: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.FileChanged)
            }

            override fun onFileDelete(file: File) {
               emitChangeEvent(directory, FileSystemChangeEventType.FileDeleted)
            }

            override fun onStop(observer: FileAlterationObserver) {
               logger.debug("File poll completed")
            }

         })
      }
      monitor = FileAlterationMonitor(pollFrequency.toMillis())
      monitor.addObserver(observer)
   }

   private fun emitChangeEvent(file: File, type: FileSystemChangeEventType) {
      logger.info { "File changed: $type at ${file.canonicalPath}" }
      sink.emitNext(listOf(FileSystemChangeEvent(file.toPath(), type))) { signalType, emitResult ->
         logger.warn { "Failed to emit FileSystemChangeEvent: $signalType $emitResult" }
         false
      }
   }

   override fun startWatching(): Flux<List<FileSystemChangeEvent>> {
      monitor.start()
      return sink.asFlux()
   }

   fun pollNow() {
      observer.checkAndNotify()
   }
}
