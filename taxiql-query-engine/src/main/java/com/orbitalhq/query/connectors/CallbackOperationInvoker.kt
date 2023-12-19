package com.orbitalhq.query.connectors

import com.orbitalhq.models.*
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.*
import com.orbitalhq.schemas.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.formatter.TaxiCodeFormatter
import mu.KotlinLogging
import java.time.Instant

/**
 * Allows passing a map of responses, keyed off an Id.
 * It's expected the Id is passed as the first param in the operation.
 * Used for quickly prepping multiple responses to things like:
 *
 * findByThing(Thing):OtherThing
 */
fun responsesById(responses: Map<Any, TypedInstance>): OperationResponseHandler {
    return { remoteOperation, inputs ->
        val keyParameter = inputs.firstOrNull() ?: error("Expected to receive an input parameter, but didn't")
        val keyValue = keyParameter.second.value
        val response = responses[keyValue] ?: error("No response was provided for key $keyValue")
        listOf(response)
    }
}

fun responsesById(idField: String, responses: List<TypedInstance>): OperationResponseHandler {
    val responsesMap = responses.associateBy { typedInstance ->
        val key = (typedInstance as TypedObject)[idField].value
        key!!
    }
    return responsesById(responsesMap)
}

fun responsesToTaxiQlById(responses: List<TypedInstance>): OperationResponseHandler {
    // Group the responses by the IdField
    return { remoteOperation, inputs ->
        val taxiQLQueryDataSource = inputs.first().second.source as ConstructedQueryDataSource
        val idValue = taxiQLQueryDataSource.inputs.single()
        val matched = responses.first { response ->
            val thisInstanceValue = (response as TypedObject).getAttributeIdentifiedByType(idValue.type)
            thisInstanceValue.valueEquals(idValue)
        }
        listOf(matched)
    }
}

typealias OperationResponseHandler = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<TypedInstance>
typealias OperationResponseFlowProvider = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> Flow<TypedInstance>

