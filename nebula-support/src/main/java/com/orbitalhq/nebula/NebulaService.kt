package com.orbitalhq.nebula

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import reactor.core.publisher.Mono

@RestController
class NebulaService(
    private val submissionService: NebulaSubmissionService
) {

    private var lastNebulaStateUpdatedEvent: NebulaStateUpdatedEvent = NebulaStateUpdatedEvent.empty()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        submissionService.stateUpdates
            .subscribe { event ->
                this.lastNebulaStateUpdatedEvent = event
            }
    }


    @GetMapping("/api/stubs")
    fun getNebulaStack(): NebulaStateUpdatedEvent {
        return lastNebulaStateUpdatedEvent
    }


}

typealias NebulaStackState = Map<StackName, Map<ComponentType, ComponentInfo>>
typealias NebulaEnvVariablesMap = Map<StackName, Map<ComponentType, Map<EnvVarKey, EnvVarValue>>>
typealias StackName = String
typealias ComponentType = String
typealias EnvVarKey = String
typealias EnvVarValue = String

/**
 * Note that the nebula entry here should be resolved via services.conf
 */
@HttpExchange("http://nebula")
interface NebulaApi {
    /**
     * Returns the names of current stacks
     */
    @GetExchange("/stacks")
    fun getStacks(): Mono<NebulaStackState>

    /**
     * Creates a new stack.
     * Returns the newly created stack name
     */
    @PostExchange("/stacks")
    fun postStack(@RequestBody scriptSource: String): Mono<String>

    /**
     * Updates an existing stack.
     * Returns the updated stacks name
     */
    @PutExchange("/stacks/{name}")
    fun updateStack(@PathVariable("name") name: String, @RequestBody script: String): Mono<String>

    /**
     * Deletes a stack.
     * Returns the list of remaining stack names.
     */
    @DeleteExchange("/stacks/{name}")
    fun deleteStack(@PathVariable("name") name: String): Mono<List<String>>
}

// copy of Nebula's Component info
data class ComponentInfo(
    val container: ContainerInfo?,
    val componentConfig: Map<String, Any>
)

data class ContainerInfo(
    val containerId: String,
    val imageName: String,
    val containerName: String,
)
