package io.vyne.pipelines.runner

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.EmitterPipelineTransportHealthMonitor
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.PipelineInputMessage
import io.vyne.pipelines.PipelineInputTransport
import io.vyne.pipelines.PipelineLogger
import io.vyne.pipelines.PipelineMessage
import io.vyne.pipelines.PipelineOutputTransport
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.DOWN
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus.UP
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import org.junit.Before
import org.junit.Test
import reactor.core.publisher.Flux


class PipelineInstanceTest {

   class DummyInput(override val feed: Flux<PipelineInputMessage>) : PipelineInputTransport {
      override val healthMonitor= EmitterPipelineTransportHealthMonitor()
      override val description: String = "Dummy Input"
      override fun type(schema: Schema): Type {
         TODO("Not yet implemented")
      }
   }
   class DummyOutput(val type: VersionedTypeReference) : PipelineOutputTransport {
      override fun write(message: MessageContentProvider, logger: PipelineLogger, schema: Schema) { }
      override val healthMonitor = EmitterPipelineTransportHealthMonitor()
      override val description: String = "Dummy Output"
      override fun type(schema: Schema): Type {
         TODO("Not yet implemented")
      }
   }

   lateinit var input: PipelineInputTransport
   lateinit var output: PipelineOutputTransport
   lateinit var flux: Flux<PipelineMessage>
   lateinit var instance: PipelineInstance;

   @Before
   fun setup() {
      flux =  mock();
      whenever(flux.subscribe()).thenReturn(mock())

      input = spy(DummyInput(mock()))
      output = spy(DummyOutput(mock()))

      instance = PipelineInstance(
         spec = mock(),
         instancesFeed = flux,
         startedTimestamp = mock(),
         input = input,
         output = output
      )
   }

   @Test
   fun testBothInputUp() {
      // Events [input UP, output UP]
      input.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(UP)

      // We should subscribe
      verify(flux).subscribe()
      verify(input, never()).pause()
   }

   @Test
   fun testOnlyInputUp() {
      // Events [input UP]
      input.healthMonitor.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testOnlyOutputUp() {
      // Events [output UP]
      output.healthMonitor.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testInputUpDownOutputUp() {
      // Events [input UP, input DOWN, input UP]
      input.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(DOWN)
      output.healthMonitor.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testOutputUpDownOutputUp() {
      // Events [output UP, output DOWN, output UP]
      output.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(DOWN)
      output.healthMonitor.reportStatus(UP)

      // We shouldn't subscribe
      verify(flux, never()).subscribe()
   }

   @Test
   fun testBothUpThenInputDown() {
      // Events [input UP, output UP, input DOWN]
      input.healthMonitor.reportStatus(UP)
      output.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(DOWN)

      // We should subscribe
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, never()).resume()
   }

   @Test
   fun testBothUpThenOuptutDown() {
      // Events [output UP,  input UP, output DOWN]
      output.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(DOWN)

      // We should subscribe, pause and never resume
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, never()).resume()
   }

   @Test
   fun testBothUpThenOneDownThenUp() {
      // Events [output UP,  input UP, output DOWN, output UP]
      output.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(UP)
      input.healthMonitor.reportStatus(DOWN)
      input.healthMonitor.reportStatus(UP)

      // We should subscribe, pause and resume
      verify(flux, times(1)).subscribe()
      verify(input, times(1)).pause()
      verify(input, times(1)).resume()
   }
}

