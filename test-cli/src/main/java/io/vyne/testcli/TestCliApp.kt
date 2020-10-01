package io.vyne.testcli

import io.vyne.testcli.commands.ExecuteTestCommand
import picocli.CommandLine
import picocli.CommandLine.Spec
import kotlin.system.exitProcess

@CommandLine.Command(
   name = "testcli",
   description = ["Vyne test cli"],
   subcommands = [
      ExecuteTestCommand::class
   ],
   versionProvider = VersionInfoProvider::class
)
class TestCliApp : Runnable {

   @CommandLine.Option(names = ["-v", "--version"], versionHelp = true, description = ["print version information"])
   var versionRequested = false

   @Spec
   var spec: CommandLine.Model.CommandSpec? = null

   override fun run() {
      throw CommandLine.ParameterException(spec?.commandLine(), "Specify a subcommand")
   }
}

fun main(vararg args: String) {
   val exitCode = CommandLine(TestCliApp()).execute(*args)
   exitProcess(exitCode)
}
