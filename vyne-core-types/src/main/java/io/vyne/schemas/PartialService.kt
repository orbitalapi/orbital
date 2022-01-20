package io.vyne.schemas

import lang.taxi.Operator
import lang.taxi.services.QueryOperationCapability

interface PartialService {
   val name: QualifiedName
   val operations: List<PartialOperation>
   val queryOperations: List<PartialQueryOperation>
   val metadata: List<Metadata>
   val typeDoc: String?
}

interface PartialOperation {
   val qualifiedName: QualifiedName
   val parameters: List<PartialParameter>
   val metadata: List<Metadata>
   val typeDoc: String?
   val returnTypeName: QualifiedName
}

interface PartialParameter {
   val name: String?
   val typeName: QualifiedName
   val metadata: List<Metadata>
}

interface PartialQueryOperation : PartialOperation {
   val grammar: String
   val capabilities: List<QueryOperationCapability>
   val hasFilterCapability: Boolean
   val supportedFilterOperations: List<Operator>
}
