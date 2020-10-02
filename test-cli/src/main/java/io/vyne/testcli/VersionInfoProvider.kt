package io.vyne.testcli

import com.google.common.io.Resources
import org.springframework.core.io.support.PropertiesLoaderUtils
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.util.*

class VersionInfoProvider : CommandLine.IVersionProvider {
   val version:String
   init {
      version = try {
         val properties = PropertiesLoaderUtils.loadAllProperties("git.properties")
         val gitSha = properties["git.commit.id.abbrev"] as String
         val version = properties["git.build.version"] as String
         "v$version ($gitSha)"
      } catch (exception:Throwable) {
         "developer build"
      }
   }

   override fun getVersion(): Array<String> {
      return arrayOf(version)
   }

}
