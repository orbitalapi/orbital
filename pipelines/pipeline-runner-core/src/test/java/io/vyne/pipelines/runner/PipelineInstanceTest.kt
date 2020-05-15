package io.vyne.pipelines.runner

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.*
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.pipelines.runner.transport.direct.DirectInput
import io.vyne.pipelines.runner.transport.direct.DirectOutput
import org.junit.Before
import org.junit.Test
import reactor.core.publisher.Flux

class PipelineInstanceTest {

   class DummyInput(override val feed: Flux<PipelineInputMessage>) : PipelineInputTransport, AbstractPipelineTransportHealthMonitor()
   class DummyOutput(override val type: VersionedTypeReference) : PipelineOutputTransport, AbstractPipelineTransportHealthMonitor() {
      override fun write(typedInstance: TypedInstance, logger: PipelineLogger) { }
   }

   lateinit var input: PipelineInputTransport
   lateinit var output: PipelineOutputTransport
   lateinit var flux: Flux<TypedInstance>
   lateinit var instance: PipelineInstance;

   @Before
   fun setup() {
      flux =  mock();
      input = spy(DummyInput(mock()))
      output = spy(DummyOutput(mock()))

      instance = PipelineInstance(
         spec = mock(),
         flux = flux ,
         startedTimestamp = mock(),
         input = input,
         output = output
      )
   }

   @Test
   fun testBothInputUp() {
      // Events [input UP, output UP]
      input.reportStatus(UP)
      output.reportStatus(UP)

      // We should subscribe
      verify(flux).subscribe()
      verify(input, never()).pause()
   }

   @Test
   fun testOnlyInputUp() {
      // Events [input UP]
      input.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testOnlyOutputUp() {
      // Events [output UP]
      output.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testInputUpDownOutputUp() {
      // Events [input UP, input DOWN, input UP]
      input.reportStatus(UP)
      input.reportStatus(DOWN)
      output.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testOutputUpDownOutputUp() {
      // Events [output UP, output DOWN, output UP]
      output.reportStatus(UP)
      output.reportStatus(DOWN)
      output.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testBothUpThenInputDown() {
      // Events [input UP, output UP, input DOWN]
      input.reportStatus(UP)
      output.reportStatus(UP)
      input.reportStatus(DOWN)

      // We should subscribe
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, never()).resume()
   }

   @Test
   fun testBothUpThenOuptutDown() {
      // Events [output UP,  input UP, output DOWN]
      output.reportStatus(UP)
      input.reportStatus(UP)
      input.reportStatus(DOWN)

      // We should subscribe, pause and never resume
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, never()).resume()
   }

   @Test
   fun testBothUpThenOneDownThenUp() {
      // Events [output UP,  input UP, output DOWN, output UP]
      output.reportStatus(UP)
      input.reportStatus(UP)
      input.reportStatus(DOWN)
      input.reportStatus(UP)

      // We should subscribe, pause and resume
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, times(1)).resume()
   }
}
