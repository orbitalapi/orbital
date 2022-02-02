package io.vyne.schemaServerCore.file

import com.google.common.io.Resources
import org.apache.commons.io.FileUtils
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path

/**
 * Copies a taxi project in resources to the temp folder.
 * Sample usage:
 *
 * ```kotlin
 * @Rule
 * @JvmField
 * val projectHome = TemporaryFolder()
 *
 * projectHome.deployProject("sample-project")
 * ```
 */
fun TemporaryFolder.deployProject(path:String):Path {
   val testProject = File(Resources.getResource(path).toURI())
   FileUtils.copyDirectory(testProject, this.root)
   return this.root.toPath()
}
