package com.orbitalhq.internal.packagePublisher

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
   name = "Internal schema extractor",
   mixinStandardHelpOptions = true,
   version = [""],
   description = ["Extracts and publishes Orbitals internal schemas"]
)
class OrbitalPackagePublisher : Callable<Int> {
   @CommandLine.Spec
   private lateinit var spec: CommandLine.Model.CommandSpec

   private fun printOut(message: String) {
      val commandLine = spec.commandLine()
      commandLine.out.println(message)
   }

   @Option(names = ["--output", "-o"], description = ["Taxi project directory"], arity = "1", required = true)
   private var outputPath: String? = null

   @Option(
      names = ["--version", "-v"],
      description = ["Defines the version, will update the taxi.conf file"],
      arity = "1",
      required = true
   )
   private var version: String? = null


   @Option(names = ["--commit"], description = ["Commit changes"], defaultValue = "false")
   private var commit: Boolean = false

   @Option(names = ["--push"], description = ["Push changes"], defaultValue = "false")
   private var push: Boolean = false

   @Option(names = ["--tag"], description = ["Add tag"], required = false)
   private var tag: String? = null

   @Option(
      names = ["--ssh-key"],
      description = ["Path to SSH private key"],
      defaultValue = "\${sys:user.home}/.ssh/id_rsa"
   )
   private lateinit var sshKeyPath: String

   @Option(
      names = ["--remote-url"],
      description = ["Git Repository Remote URL"],
      required = false
   )
   private var remoteRepositoryUrl: String? = null

   override fun call(): Int {
      val config = OrbitalPackagePublisherConfig(
         outputPath = outputPath!!,
         version = version!!,
         commitChanges = commit,
         pushChanges = push,
         tagName = tag,
         sshKeyPath = sshKeyPath,
         remoteRepositoryUrl = remoteRepositoryUrl
      )
      BuiltInsSourcePackageWriter(config, spec.commandLine()).write()
      return 0
   }
}

data class OrbitalPackagePublisherConfig(val outputPath: String,
                                         val version: String,
                                         val commitChanges: Boolean,
                                         val pushChanges: Boolean,
                                         val tagName: String?,
                                         val sshKeyPath: String,
                                         val remoteRepositoryUrl: String?)


fun main(args: Array<String>): Unit = exitProcess(CommandLine(OrbitalPackagePublisher()).execute(*args))
