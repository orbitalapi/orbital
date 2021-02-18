package io.vyne.testcli.commands

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.testcli.commands.ExecuteTestCommand.Companion.TEST_FAILED
import io.vyne.testcli.commands.ExecuteTestCommand.Companion.TEST_SUCCESSFUL
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class QueryTestCommandTest {
   @Test
   fun `executes a query specification successfully`() {
      val resource = Resources.getResource("query-tests/bank.order.Report")
      val root = File(resource.toURI())
      val command = QueryTestCommand().apply {
         specPath = root.toPath()
      }
      command.call().should.equal(TEST_SUCCESSFUL)
   }

   @Test
   fun `detects failed tests`() {
      val resource = Resources.getResource("query-tests/bank.order.Report.failed")
      val root = File(resource.toURI())
      val command = QueryTestCommand().apply {
         specPath = root.toPath()
      }
      command.call().should.equal(TEST_FAILED)
   }
}
