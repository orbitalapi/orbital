package io.vyne.schemas

import lang.taxi.expressions.Expression

interface PartialSchema {
   val types: Set<out PartialType>
   val services: Set<PartialService>

   fun type(name: QualifiedName): PartialType
}

interface PartialType {
   val name: QualifiedName
   val attributes: Map<AttributeName, Field>
   val modifiers: List<Modifier>
   val metadata: List<Metadata>
   val inheritsFromTypeNames: List<QualifiedName>
   val enumValues: List<EnumValue>
   val typeParametersTypeNames: List<QualifiedName>
   val typeDoc: String?

   val isPrimitive: Boolean
   val isEnum: Boolean
   val isCollection: Boolean
   val isScalar: Boolean

   val fullyQualifiedName: String
   val basePrimitiveTypeName: QualifiedName?
   val format: List<String>?
   val declaresFormat: Boolean

   val unformattedTypeName:QualifiedName?
   val offset:Int?
   val expression:Expression?
}
