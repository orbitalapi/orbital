package io.vyne.testcli.commands

import com.google.common.io.Resources
import com.winterbe.expekt.should
import org.junit.Test
import java.nio.file.Paths

class ExecuteSpecCommandTest {

   @Test
   fun `executes all specs under directory`() {
      val spec = Resources.getResource("simple-test/specs")
      val testResults = ExecuteSpecCommand().apply {
         specPath = Paths.get(spec.toURI())
      }.executeTests()
         .toList()
      testResults.should.have.size(2)
      testResults.count { it.successful }.should.equal(1)
      testResults.count { !it.successful }.should.equal(1)
   }

   @Test
   fun `simple project gets executed`() {
      val spec = Resources.getResource("simple-test/specs/hello-world/hello-world.spec.conf")
      val testResult = ExecuteSpecCommand().apply {
         specPath = Paths.get(spec.toURI())
      }.executeTests().toList().first()
      testResult.successful.should.be.`true`
   }

   @Test
   fun `captures failure`() {
      val spec = Resources.getResource("simple-test/specs/expect-to-fail")
      val testResult = ExecuteSpecCommand().apply {
         specPath = Paths.get(spec.toURI())
      }.executeTests().toList().first()
      testResult.successful.should.be.`false`
      testResult.failure!!.message.should.contain("Expected: vyne.demo.LastName")
      testResult.failure!!.message.should.contain("got: vyne.demo.FirstName")
   }

   @Test
   fun `reads spec correctly`() {
      val specUrl = Resources.getResource("simple-test/specs/hello-world/hello-world.spec.conf")
      val spec = ExecuteSpecCommand().apply {
         specPath = Paths.get(specUrl.toURI())
      }.buildTestSpecs().toList().first().spec!!
      spec.name.should.equal("simple hello world")
      spec.targetType.should.equal("vyne.demo.Person")
   }

   @Test
   fun `finds taxi conf in parent folder and compiles project`() {
      val spec = Resources.getResource("simple-test/specs/hello-world/hello-world.spec.conf")
      val taxi = ExecuteSpecCommand().apply {
         specPath = Paths.get(spec.toURI())
      }.buildProject()
      taxi.containsType("vyne.demo.Person").should.be.`true`
   }
}
