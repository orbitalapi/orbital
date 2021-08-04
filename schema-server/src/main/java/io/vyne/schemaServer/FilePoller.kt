package io.vyne.schemaServer

import mu.KLogger
import mu.KotlinLogging
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.HiddenFileFilter
import org.apache.commons.io.filefilter.IOFileFilter
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@ConditionalOnProperty(
   name = ["taxi.change-detection-method"],
   havingValue = "poll",
   matchIfMissing = false
)
@Component
class FilePoller(
   @Value("\${taxi.schema-local-storage}") private val schemaLocalStorage: String,
   @Value("\${taxi.schema-poll-interval-seconds:5}") private val pollIntervalSeconds: Int,
   @Value("\${taxi.schema-increment-version-on-recompile:true}") private val incrementVersionOnRecompile: Boolean,
   private val compilerService: CompilerService,
   @Suppress("SpringJavaInjectionPointsAutowiringInspection")
   private val logger: KLogger = KotlinLogging.logger {},
) : AutoCloseable {


   private var monitor: FileAlterationMonitor

   init {
      val path: Path = Paths.get(schemaLocalStorage)

      logger.info("Creating a file poller at ${path.toFile().canonicalPath}")
      val directories: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.directoryFileFilter(),
         HiddenFileFilter.VISIBLE)
      val taxiFiles: IOFileFilter = FileFilterUtils.and(
         FileFilterUtils.fileFileFilter(),
         FileFilterUtils.suffixFileFilter(".taxi"))
      val filter: IOFileFilter = FileFilterUtils.or(directories, taxiFiles)
      val observer = FileAlterationObserver(path.toFile(), filter).apply {
         addListener(object : FileAlterationListener {
            override fun onStart(observer: FileAlterationObserver) {
               logger.info("File poll starting")
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
               logger.info("File poll completed")
            }

         })
      }
      monitor = FileAlterationMonitor((pollIntervalSeconds * 1000).toLong())
      monitor.addObserver(observer)
      monitor.start()

   }

   private fun recompile(eventMessage: String) {
      logger.info(eventMessage)
      compilerService.recompile(incrementVersionOnRecompile)
   }

   override fun close() = monitor.stop()
}
