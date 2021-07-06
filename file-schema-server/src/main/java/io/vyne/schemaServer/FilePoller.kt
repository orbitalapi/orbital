package io.vyne.schemaServer

import io.vyne.utils.log
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
   private val localFileSchemaPublisherBridge: LocalFileSchemaPublisherBridge
) {

   init {
      val path: Path = Paths.get(schemaLocalStorage)

      log().info("Creating a file poller at ${path.toFile().canonicalPath}")
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
               log().info("File poll starting")
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
               log().info("File poll completed")
            }

         })
      }
      val monitor = FileAlterationMonitor((pollIntervalSeconds * 1000).toLong())
      monitor.addObserver(observer)
      monitor.start()

   }

   private fun recompile(eventMessage: String) {
      log().info(eventMessage)
      localFileSchemaPublisherBridge.rebuildSourceList()
   }

}
