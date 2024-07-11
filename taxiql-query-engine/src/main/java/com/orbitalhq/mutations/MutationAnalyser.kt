package com.orbitalhq.mutations

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.mutations.Mutation

data class MutationProps(val isStreamingQuery: Boolean,
                         val mutationOperationHasCollectionParameters: Boolean,
                         val isValid: Boolean,
                         val validationError: String) {

    companion object {
        fun from(mutation: Mutation?, targetType: Type, schema: Schema): MutationProps? {
            return mutation?.let {
                val service = schema.service(mutation.service.qualifiedName)
                val operation = service.operation(mutation.operation.name)
                val isStreamingQuery = targetType.isStream
                val mutationOperationHasCollectionParameters = operation.parameters.any { parameter -> parameter.type.isCollection }
                val isValid = !(isStreamingQuery && mutationOperationHasCollectionParameters)
                val validationError = if (isValid) {
                    ""
                } else {
                    val parameterizedTypeName = operation.parameters.first { parameter -> parameter.type.isCollection }.type.paramaterizedName
                    "Unable perform mutation operation to $parameterizedTypeName from  ${targetType.paramaterizedName}"

                }

                MutationProps(
                    isStreamingQuery = isStreamingQuery,
                    mutationOperationHasCollectionParameters = mutationOperationHasCollectionParameters,
                    isValid = !(isStreamingQuery && mutationOperationHasCollectionParameters),
                    validationError = validationError
                )
            }

        }
    }
}