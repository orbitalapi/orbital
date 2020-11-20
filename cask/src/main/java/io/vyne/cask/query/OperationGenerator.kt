package io.vyne.cask.query

import io.vyne.cask.query.generators.OperationAnnotation
import lang.taxi.services.Operation
import lang.taxi.services.ServiceMember
import lang.taxi.types.Field
import lang.taxi.types.Type

interface OperationGenerator {
   fun generate(field: Field?, type: Type): Operation
   fun canGenerate(field: Field, type: Type): Boolean
   fun expectedAnnotationName(): OperationAnnotation
}

interface DefaultOperationGenerator {
   fun canGenerate(returnType: Type): Boolean {
      return true
   }
   fun generate(returnType: Type): ServiceMember

}
