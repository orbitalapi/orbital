package io.vyne.licensing.vendor

import io.vyne.licensing.License
import io.vyne.licensing.LicensedEdition
import io.vyne.licensing.Signing
import org.beryx.textio.TextIoFactory
import picocli.CommandLine
import picocli.CommandLine.Option
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import kotlin.system.exitProcess

class LicenseCli {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         CommandLine(CliOptions())
            .setOptionsCaseInsensitive(true)
            .execute(*args)
      }
   }
}

class CliOptions : Runnable {
   val textIO = TextIoFactory.getTextIO()

   @Option(names = ["-k", "--key"], description = ["Path to private key"])
   lateinit var pathToKey: String

   @Option(names = ["-n", "--name"], description = ["Licensee name"], interactive = true, echo = true)
   lateinit var name: String

   @Option(
      names = ["-x", "--expires"],
      description = ["When the license expires"],
      interactive = true,
      echo = true
   )
   lateinit var expiresOn: Instant

   @Option(
      names = ["-e", "--edition"],
      description = ["Licensed Edition. Valid values: \${COMPLETION-CANDIDATES}."],
      interactive = true,
      echo = true
   )
   lateinit var edition: LicensedEdition

   @Option(
      names = ["-o", "--output"],
      description = ["Output filename. Default: \${DEFAULT-VALUE}"],
      interactive = true,
      echo = true
   )
   var path: Path = Paths.get("./license.json")


   override fun run() {
      val privateKey = Paths.get(pathToKey).toFile().readBytes()
      val vendor = LicenseVendor.forPrivateKey(privateKey)

      promptForMissingInputs()

      val license = License(
         name!!,
         expiresOn,
         Instant.now(),
         edition
      )
      val signedLicense = vendor.generateSignedLicense(license)
      val licenseJson = Signing.objectMapper.writerWithDefaultPrettyPrinter()
         .writeValueAsString(signedLicense)
      val file = path.toFile()
      if (file.exists()) {
         file.delete()
      }
      file.writeText(licenseJson)
      println("Wrote license file to ${file.canonicalPath}")
      exitProcess(0)
   }

   private fun promptForMissingInputs() {

      if (!this::name.isInitialized) {
         this.name = prompt("Provide licensee name")
      }
      if (!this::expiresOn.isInitialized) {
         this.expiresOn = prompt("License expiration", Instant::parse, Instant.now().plus(Duration.ofDays(365)))
      }
      if (!this::edition.isInitialized) {
         fun parse(input: String): LicensedEdition {
            return when (input.toLowerCase().first().toString()) {
               "e" -> LicensedEdition.ENTERPRISE
               "p" -> LicensedEdition.PLATFORM
               "s" -> LicensedEdition.STARTER
               else -> error("Not a valid option")
            }
         }
         this.edition =
            prompt("Edition.  Valid values = (E)nterprise, (P)latform, (S)tarter", ::parse, LicensedEdition.STARTER)
      }
   }

   private fun <T> prompt(message: String, parse: (String) -> T = { it as T }, defaultValue: T? = null): T {
      val prompt = if (defaultValue != null) {
         "$message ($defaultValue) : "
      } else {
         "$message : "
      }
      val input = textIO.newStringInputReader().let {
         if (defaultValue != null) {
            it.withDefaultValue(defaultValue!!.toString())
         } else {
            it
         }
      }.read(message)
      if (input.isNullOrEmpty()) {
         return if (defaultValue == null) {
            println("This is required")
            prompt(message, parse, defaultValue)
         } else {
            defaultValue;
         }
      }
      return parse(input)
   }
}
