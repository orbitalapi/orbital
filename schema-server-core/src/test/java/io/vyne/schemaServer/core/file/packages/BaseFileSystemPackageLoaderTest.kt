package io.vyne.schemaServer.core.file.packages

import io.vyne.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.Duration

open class BaseFileSystemPackageLoaderTest {


   protected val factory = SchemaSourcesAdaptorFactory()

   @Rule
   @JvmField
   val projectHome = TemporaryFolder()

   protected fun buildLoader(packageSpec: FileSystemPackageSpec): Pair<ReactivePollingFileSystemMonitor, FileSystemPackageLoader> {
      val fileMonitor = ReactivePollingFileSystemMonitor(
         packageSpec.path,
         Duration.ofDays(100L) // Poll Manually
      )
      val loader = FileSystemPackageLoader(
         packageSpec,
         factory.getAdaptor(packageSpec.loader),
         fileMonitor,
      )

      return fileMonitor to loader

   }
}
