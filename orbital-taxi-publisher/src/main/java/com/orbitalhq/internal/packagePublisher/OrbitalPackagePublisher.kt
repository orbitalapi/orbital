package com.orbitalhq.internal.packagePublisher

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

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

   @Option(names = ["--output", "-o"], description = ["Taxi project directory"], arity = "1")
   private var outputPath: String? = null

   @Option(
      names = ["--version", "-v"],
      description = ["Defines the version, will update the taxi.conf file"],
      arity = "1"
   )
   private var version: String? = null


   @Option(names = ["--commit"], description = ["Commit changes"])
   private var commit: Boolean = false

   @Option(names = ["--push"], description = ["Push changes"])
   private var push: Boolean = false

   @Option(names = ["--tag"], description = ["Add tag"])
   private var tag: String? = null

   @Option(
      names = ["--ssh-key"],
      description = ["Path to SSH private key"],
      defaultValue = "\${sys:user.home}/.ssh/id_rsa"
   )
   private lateinit var sshKeyPath: String

   override fun call(): Int {
      BuiltInsSourcePackageWriter(outputPath!!, version!!, spec.commandLine())
         .write()
      return 0
   }


}

fun main(args: Array<String>): Unit = System.exit(CommandLine(OrbitalPackagePublisher()).execute(*args))
