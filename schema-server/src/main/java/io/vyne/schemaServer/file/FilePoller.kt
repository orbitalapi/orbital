package io.vyne.schemaServer.file

import mu.KotlinLogging
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.HiddenFileFilter
import org.apache.commons.io.filefilter.IOFileFilter
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import java.io.File
import java.nio.file.Path
import java.time.Duration

class FilePoller(
   override val repository: FileSystemSchemaRepository,
   private val pollDuration: Duration
) : FileSystemMonitor {

   private val logger = KotlinLogging.logger {}

   private val monitor: FileAlterationMonitor
   private val observer: FileAlterationObserver
   init {
      val path: Path = repository.projectPath

      logger.info("Creating a file poller at ${path.toFile().canonicalPath}")
      val directories: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.directoryFileFilter(),
         HiddenFileFilter.VISIBLE
      )
      val taxiFiles: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.fileFileFilter(),
         FileFilterUtils.suffixFileFilter(".taxi")
      )
      val filter: IOFileFilter = FileFilterUtils.or(directories, taxiFiles)
      observer = FileAlterationObserver(path.toFile(), filter).apply {
         addListener(object : FileAlterationListener {
            override fun onStart(observer: FileAlterationObserver) {
               logger.debug("File poll starting")
            }

            override fun onDirectoryCreate(directory: File) {
               recompile("Directory created at ${directory.canonicalPath} - recompiling")
            }

            override fun onDirectoryChange(directory: File) {
               recompile("Directory changed at ${directory.canonicalPath} - recompiling")
            }

            override fun onDirectoryDelete(directory: File) {
               recompile("Directory deleted at ${directory.canonicalPath} - recompiling")
            }

            override fun onFileCreate(file: File) {
               recompile("File created at ${file.canonicalPath} - recompiling")
            }

            override fun onFileChange(file: File) {
               recompile("File changed at ${file.canonicalPath} - recompiling")
            }

            override fun onFileDelete(file: File) {
               recompile("File deleted at ${file.canonicalPath} - recompiling")
            }

            override fun onStop(observer: FileAlterationObserver) {
               logger.debug("File poll completed")
            }

         })
      }
      monitor = FileAlterationMonitor(pollDuration.toMillis())
      monitor.addObserver(observer)
   }

   override fun start() {
      monitor.start()
   }

   override fun stop() {
      monitor.stop()
   }

   fun poll() {
      observer.checkAndNotify()
   }

   private fun recompile(eventMessage: String) {
      logger.info(eventMessage)
      repository.refreshSources()
   }
}
