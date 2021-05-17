package io.vyne.regression

import com.google.common.io.Resources
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.fail


@Ignore("Deprecated, and should be removed")
class QueryTest {
   @Test
   fun testQueryExecutionScenarios() {
      val resource = Resources.getResource("scenarios")
      val root = File(resource.toURI())
      val testFailures = QueryTester().runTest(root)
      if (testFailures!!.isNotEmpty()) {
         val failureMessages = testFailures.joinToString("\n") { it.toString() }
         fail(failureMessages)
      }
   }
}

