package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.services.Operation
import lang.taxi.services.Parameter
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindByMultipleGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val returnType = TemporalFieldUtils.collectionTypeOf(type)
      val parameter = Parameter(
         annotations = listOf(Annotation("RequestBody", mapOf())),
         type = TemporalFieldUtils.collectionTypeOf(TemporalFieldUtils.parameterType(field)),
         name = field.name,
         constraints = listOf())
      //@HttpOperation(method = "POST" , url = "/api/cask/findOneBy/ion/trade/Trade/orderId/{cacib.orders.OrderId}")
      //operation findMultipleByOrderId( @RequestBody orderId : cacib.orders.OrderId[] ) : ion.trade.Trade[]
      return Operation(
         name = "findMultipleBy${field.name.capitalize()}",
         parameters = listOf(parameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "POST", "url" to getfindMultipleByRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return PrimitiveType.isAssignableToPrimitiveType(field.type) && TemporalFieldUtils.annotationFor(field, ExpectedAnnotationName) != null
   }

   private fun getfindMultipleByRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return "${CaskServiceSchemaGenerator.CaskApiRootPath}findMultipleBy/${path.parts.joinToString("/")}/${field.name}"
   }


   companion object {
      const val ExpectedAnnotationName = "UniqueId"
   }
}
