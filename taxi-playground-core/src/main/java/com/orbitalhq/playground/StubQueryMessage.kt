package com.orbitalhq.playground

data class StubQueryMessage(
   val schema: String,
   val query: String,
   val parameters: Map<String,Any> = emptyMap(),
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
   val response: String
)

typealias FakeFilePath = String
typealias FileContents = String

data class StubQueryProject(
   val files: Map<FakeFilePath,FileContents>
)
