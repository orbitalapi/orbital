package com.orbitalhq.query.connectors

import arrow.core.Either
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.right
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import kotlinx.coroutines.flow.Flow

/**
 * Allows passing a map of responses, keyed off an Id.
 * It's expected the Id is passed as the first param in the operation.
 * Used for quickly prepping multiple responses to things like:
 *
 * findByThing(Thing):OtherThing
 */
fun responsesById(responses: Map<Any, TypedInstance>): (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<Either<StreamErrorMessage, TypedInstance>> {
    return { remoteOperation, inputs ->
        val keyParameter = inputs.firstOrNull() ?: error("Expected to receive an input parameter, but didn't")
        val keyValue = keyParameter.second.value
        val response = responses[keyValue] ?: error("No response was provided for key $keyValue")
        listOf(response.right())
    }
}

fun responsesById(idField: String, responses: List<TypedInstance>): (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<Either<StreamErrorMessage, TypedInstance>> {
    val responsesMap = responses.associateBy { typedInstance ->
        val key = (typedInstance as TypedObject)[idField].value
        key!!
    }
    return responsesById(responsesMap)
}

fun responsesToTaxiQlById(responses: List<TypedInstance>): (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<Either<StreamErrorMessage, TypedInstance>> {
    // Group the responses by the IdField
    return { remoteOperation, inputs ->
        val taxiQLQueryDataSource = inputs.first().second.source as ConstructedQueryDataSource
        val idValue = taxiQLQueryDataSource.inputs.single()
        val matched = responses.first { response ->
            val thisInstanceValue = (response as TypedObject).getAttributeIdentifiedByType(idValue.type)
            thisInstanceValue.valueEquals(idValue)
        }
        listOf(matched.right())
    }
}

typealias OperationResponseHandler = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> List<Either<StreamErrorMessage, TypedInstance>>
typealias OperationResponseFlowProvider = (RemoteOperation, List<Pair<Parameter, TypedInstance>>) -> Flow<Either<StreamErrorMessage, TypedInstance>>

