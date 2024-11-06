package com.orbitalhq.playground

data class StubQueryMessage(
   val schema: String,
   val query: String,
   val parameters: Map<String, Any> = emptyMap(),
   val stubs: List<OperationStub> = emptyList(),
   val expectedJson: String? = null,
   /**
    * The nebula stack Id that's currently running for this session.
    * Null if one doesn't exist yet.
    */
   val stackId: String? = null,
   val project: StubQueryProject? = null
)

data class OperationStub(
   val operationName: String,
   val response: String,
   val echoInput: Boolean = false,
   val conditionalResponses: List<ResponseCondition> = emptyList()
)

data class ResponseCondition(
   val inputs: List<ParameterValue>,
   val response: StubbedResponse
)

data class StubbedResponse(
   val body: String,
   // later, headers etc...
)

data class ParameterValue(
   val name: String,
   val value: Any
)
typealias FakeFilePath = String
typealias FileContents = String

data class StubQueryProject(
   val files: Map<FakeFilePath, FileContents>
)
