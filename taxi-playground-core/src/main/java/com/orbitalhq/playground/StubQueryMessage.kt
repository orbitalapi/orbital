package com.orbitalhq.playground

data class StubQueryMessage(
   val schema: String,
   val query: String,
   val parameters: Map<String,Any> = emptyMap(),
   val stubs: List<OperationStub> = emptyList(),
   val expectedJson: String? = null
)
data class OperationStub(
   val operationName: String,
   val response: String
)
