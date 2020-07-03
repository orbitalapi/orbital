package io.vyne.pipelines.orchestrator.runners

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import io.vyne.pipelines.PIPELINE_METADATA_KEY
import io.vyne.pipelines.orchestrator.*
import io.vyne.pipelines.orchestrator.PipelineState.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import java.net.URI

@RunWith(MockitoJUnitRunner::class)
class PipelinesManagerTest {

   lateinit var manager: PipelinesManager

   @Mock
   lateinit var discoveryClient: DiscoveryClient

   @Mock
   lateinit var pipelineRunnerApi: PipelineRunnerApi

   @Mock
   lateinit var runningPipelineDiscoverer: RunningPipelineDiscoverer

   @Mock
   lateinit var objectMapper: ObjectMapper

   @Before
   fun setup() {
     whenever(objectMapper.convertValue(any(), eq(PipelineRunnerInstance::class.java))).thenAnswer {
        val instance = it.getArgument(0) as ServiceInstance
        PipelineRunnerInstance(instance.instanceId, instance.uri.toString())
     }

      manager = PipelinesManager(discoveryClient, pipelineRunnerApi, runningPipelineDiscoverer, objectMapper)
   }

   @Test
   fun testReloadStateNoInstances() {

      manager.reloadState()

      manager.runnerInstances.should.be.empty
   }

   @Test
   fun testReloadStateSeveralFreeInstances() {
      mockRunners(
         freeRunner("instance-1"),
         freeRunner("instance-2"),
         freeRunner("instance-3")
      )


      manager.runnerInstances.size.should.be.equal(3)
      manager.runnerInstances.map { it.instanceId }.should.be.equal(listOf("instance-1", "instance-2", "instance-3"))
      manager.pipelines.size.should.be.equal(0)
   }

   @Test
   fun testReloadSeveralBusyInstances() {
      mockRunners(
         busyRunner("runner-1", "pipeline-1"),
         busyRunner("runner-2", "pipeline-2"),
         busyRunner("runner-3", "pipeline-3")
      )

      manager.reloadState()

      manager.runnerInstances.size.should.be.equal(3)
      manager.pipelines.size.should.be.equal(3)
   }

   @Test
   fun testReloadMixBusyFreeInstances() {
      mockRunners(
         freeRunner("instance-free-1"),
         freeRunner("instance-free-2"),
         busyRunner("instance-1", "pipeline-1"),
         busyRunner("instance-2", "pipeline-2"),
         busyRunner("instance-3", "pipeline-3")
      )

      manager.reloadState()

      manager.runnerInstances.size.should.be.equal(5)
      manager.pipelines.size.should.be.equal(3)
   }

   @Test
   fun testReloadMixBusyFreeInstancesPipelineAllocated() {
      mockRunners(
         busyRunner("instance-1", "pipeline-1"),
         busyRunner("instance-2", "pipeline-2"),
         busyRunner("instance-3", "pipeline-3"),
         freeRunner("instance-free-1"),
         freeRunner("instance-free-2")
      )

      manager.reloadState()

      manager.runnerInstances.size.should.be.equal(5)
      manager.pipelines.size.should.be.equal(3)
   }

   @Test
   fun testReloadReschedulePipeline() {
      // Firstly, 2 busy instances and one free
      mockRunners(
         busyRunner("instance-1", "pipeline-1"),
         busyRunner("instance-2", "pipeline-2"),
         freeRunner("instance-3")
      )
      manager.runnerInstances.size.should.be.equal(3)
      manager.pipelines.size.should.be.equal(2)

      // instance-2 is down
      mockRunners(
         busyRunner("instance-1", "pipeline-1"),
         freeRunner("instance-3")
      )

      // pipeline-2 get re-assigned
      manager.runnerInstances.size.should.be.equal(2)
      manager.pipelines.size.should.be.equal(2)
      manager.pipelines["pipeline-1"]!!.state.should.be.equal(RUNNING)
      manager.pipelines["pipeline-1"]!!.instance!!.instanceId.should.be.equal("instance-1")
      manager.pipelines["pipeline-2"]!!.state.should.be.equal(STARTING)
      manager.pipelines["pipeline-2"]!!.instance.should.be.`null`

      // instance-1 is down
      mockRunners(
         busyRunner("instance-3", "pipeline-2")
      )

      // Pipeline 1 get re-assigned
      manager.runnerInstances.size.should.be.equal(1)
      manager.pipelines.size.should.be.equal(2)
      manager.pipelines["pipeline-1"]!!.state.should.be.equal(STARTING)
      manager.pipelines["pipeline-1"]!!.instance.should.be.`null`
      manager.pipelines["pipeline-2"]!!.state.should.be.equal(RUNNING)
      manager.pipelines["pipeline-2"]!!.instance!!.instanceId.should.be.equal("instance-3")
   }

   @Test(expected = PipelineAlreadyExistsException::class)
   fun testAddPipelineAlreadyRunning() {
      mockRunners(
         busyRunner("runner-1", "pipeline-1")
      )

      var pipelineReference = PipelineReference("pipeline-1", """ { "name": "pipeline-1" } """)
      manager.addPipeline(pipelineReference)
   }

   @Test(expected = PipelineAlreadyExistsException::class)
   fun testAddPipelineTwice() {
      try {
         var pipelineReference = PipelineReference("pipeline-1", """ { "name": "pipeline-1" } """)
         manager.addPipeline(pipelineReference)
      } catch (e: Exception) {
      }

      manager.pipelines.size.should.be.equal(1)
      var pipelineReference = PipelineReference("pipeline-1", """ { "name": "pipeline-1" } """)
      manager.addPipeline(pipelineReference)

   }

   @Test
   fun testAddPipelineNoRunner() {
      var pipelineReference = PipelineReference("runner-1", """ { "name": "runner-1" } """)
      manager.addPipeline(pipelineReference)

      manager.runnerInstances.size.should.be.equal(0)
      manager.pipelines.size.should.be.equal(1)
      manager.pipelines["runner-1"]!!.state.should.be.equal(SCHEDULED)
   }

   @Test
   fun testAddPipelineOneBusyRunner() {
      mockRunners(
         busyRunner("runner-1", "pipeline-1")
      )

      var pipelineReference = PipelineReference("pipeline-2", """ { "name": "pipeline-2" } """)
      manager.addPipeline(pipelineReference)

      // pipeline-2 is assigned
      manager.runnerInstances.size.should.be.equal(1)
      manager.pipelines.size.should.be.equal(2)
      manager.pipelines["pipeline-1"]!!.state.should.be.equal(RUNNING)
      manager.pipelines["pipeline-2"]!!.state.should.be.equal(STARTING)
   }

   @Test
   fun testAddPipelineWithFreeRunner() {
      mockRunners(
         busyRunner("runner-1", "pipeline-1"),
         freeRunner("runner-2")
      )

      var pipelineReference = PipelineReference("pipeline-2", """ { "name": "pipeline-2" } """)
      manager.addPipeline(pipelineReference)

      manager.runnerInstances.size.should.be.equal(2)
      manager.pipelines.size.should.be.equal(2)
      manager.pipelines["pipeline-1"]!!.state.should.be.equal(RUNNING)
      manager.pipelines["pipeline-2"]!!.state.should.be.equal(STARTING)
      manager.pipelines["pipeline-2"]!!.instance.should.be.`null`
   }

   private fun mockRunners(vararg runners: ServiceInstance) {
      val runners = listOf(*runners);
      // Mock runner instances
      whenever(discoveryClient.getInstances("pipeline-runner")).thenReturn(runners)

      // Mock running pipeline discovery
      val map = runners.filter { it.metadata["name"] != null }.map {
         PipelineReference(it.metadata["name"]!!, it.metadata[PIPELINE_METADATA_KEY]!!) to it
      }.toMap()

      whenever(runningPipelineDiscoverer.discoverPipelines(runners)).thenReturn(map)

      manager.reloadState()
   }

   private fun freeRunner(instanceName: String): ServiceInstance {
      return runner(instanceName)
   }

   private fun busyRunner(instanceName: String, pipelineName: String): ServiceInstance {
      val instance = runner(instanceName)
      whenever(instance.metadata).thenReturn(mapOf(
         PIPELINE_METADATA_KEY to """ {"name" : "$pipelineName"} """,
         "name" to pipelineName
      ))

      return instance
   }

   private fun runner(instanceName: String): ServiceInstance {
      val instance = mock<ServiceInstance>()
      whenever(instance.instanceId).thenReturn(instanceName)
      whenever(instance.uri).thenReturn(URI("http://${instanceName}"))
      return instance
   }


}
