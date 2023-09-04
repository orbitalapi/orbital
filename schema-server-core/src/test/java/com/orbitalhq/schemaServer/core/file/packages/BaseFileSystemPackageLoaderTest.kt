package com.orbitalhq.schemaServer.core.file.packages

import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
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
