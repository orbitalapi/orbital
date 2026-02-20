package com.orbitalhq.pipelines.jet.streams

import com.orbitalhq.VyneProvider
import com.orbitalhq.auth.authentication.VyneUser
import com.orbitalhq.auth.authentication.toVyneUser
import com.orbitalhq.auth.getAuthClaimsAsFacts
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.policyManager.ExecutionScope
import com.orbitalhq.query.policyManager.PolicyEvaluator
import com.orbitalhq.schemas.Schema
import lang.taxi.policies.PolicyOperationScope
import lang.taxi.services.OperationScope
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Re-evaluates policies for a persistent stream of results being
 * observed.
 *
 * Note - this is applied only for the observation of the stream (ie.,
 * when someone watches a stream in the UI or over a websocket), and differs
 * from the results emitted from a policy when it's executed and first evaluated.
 *
 */
@Component
class ResultStreamAuthorizationDecorator(
   private val vyneFactory: VyneProvider,
   private val policyEvaluator: PolicyEvaluator = PolicyEvaluator()
) {
   fun applyPolicies(stream: Flux<Any>, streamName: String, user: VyneUser, schema: Schema): Flux<Any> {
      val querySource = schema.queries.firstOrNull { it.name.parameterizedName == streamName } ?: return Flux.error(
         IllegalArgumentException("Stream $streamName not found in the schema")
      )
      val (query, options, querySchema) = schema.parseQuery(querySource.sources.joinToString("\n") { it.content })

      val authFacts = user.getAuthClaimsAsFacts(querySchema)
      val vyne = vyneFactory.createVyne(authFacts.toSet(), querySchema, options)

      val queryContext = vyne.buildStandalonePolicyEvaluationScope()
      val executionScope = ExecutionScope(OperationScope.READ_ONLY, PolicyOperationScope.EXTERNAL)

      val queryReturnType = querySchema.type(query.returnType)
      val instanceType = queryReturnType.collectionType ?: queryReturnType

      return stream.mapNotNull { value ->
         // first, parse back to a typed instance
         val valueAsTypedInstance = TypedInstance.from(instanceType, value, querySchema, source = Provided)
         val evaluatedTypedInstance = kotlinx.coroutines.runBlocking { policyEvaluator.evaluate(valueAsTypedInstance, queryContext, executionScope) }
         // Convert back to a raw object, since that's what we started with
         evaluatedTypedInstance.toRawObject()
      }
   }
   fun applyPolicies(stream: Flux<Any>, streamName: String, principal: Authentication, schema: Schema): Flux<Any> {
      return applyPolicies(stream, streamName, principal.toVyneUser(), schema)

   }
}
