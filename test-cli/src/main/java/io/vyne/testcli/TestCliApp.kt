package io.vyne.testcli

import io.vyne.testcli.commands.ExecuteSpecCommand
import picocli.CommandLine
import picocli.CommandLine.Spec
import kotlin.system.exitProcess

@CommandLine.Command(
   name = "testcli",
   description = ["Vyne test cli"],
   subcommands = [
      ExecuteSpecCommand::class
   ]
)
class TestCliApp : Runnable {
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
