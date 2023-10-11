package com.orbitalhq.pipelines.runner.transport

import com.mercateo.test.clock.TestClock
import com.winterbe.expekt.should
import com.orbitalhq.pipelines.jet.api.transport.DefaultVariableProvider
import com.orbitalhq.pipelines.jet.api.transport.EnvVariableSource
import com.orbitalhq.pipelines.jet.api.transport.PipelineVariableKeys
import com.orbitalhq.pipelines.jet.api.transport.StaticVariableSource
import com.orbitalhq.pipelines.jet.api.transport.VariableProvider
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DefaultVariableProviderTest {

   @Test
   fun `when variable is provided then it is returned`() {
      val variableProvider = DefaultVariableProvider(
         StaticVariableSource(
            mapOf("\$firstName" to "Jimmy")
         )
      )

      variableProvider.populate(mapOf(
         "foo.bar.FirstName" to "\$firstName"
      )).should.equal(
         mapOf("foo.bar.FirstName" to "Jimmy")
      )
   }

   @Test
   fun `when variable is not resolved then provided value is returned`() {
      VariableProvider.empty().populate(mapOf(
         "foo.bar.FirstName" to "Jimmy"
      )).should.equal(
         mapOf("foo.bar.FirstName" to "Jimmy")
      )
   }

   @Test
   fun `returns values from env value provider`() {
      val now = OffsetDateTime.now()
      val clock = TestClock.fixed(now)
      val variableProvider = DefaultVariableProvider(EnvVariableSource(clock))

      variableProvider.populate(
         mapOf("currentTime" to PipelineVariableKeys.ENV_CURRENT_TIME)
      ).should.equal(
         mapOf("currentTime" to now.toInstant())
      )

      variableProvider.populate(
         mapOf("currentTime" to PipelineVariableKeys.ENV_CURRENT_TIME_STRING)
      ).should.equal(
         mapOf("currentTime" to DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.from(ZoneOffset.UTC)).format(now.toInstant()))
      )
   }
}
