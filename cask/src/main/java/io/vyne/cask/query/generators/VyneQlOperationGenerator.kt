package io.vyne.cask.query.generators

import io.vyne.annotations.http.HttpOperations
import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.query.vyneql.VyneQlQueryService
import io.vyne.cask.services.DefaultCaskTypeProvider
import io.vyne.query.queryBuilders.VyneQlGrammar
import lang.taxi.Operator
import lang.taxi.services.FilterCapability
import lang.taxi.services.Parameter
import lang.taxi.services.QueryOperation
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class VyneQlOperationGenerator(private val typeProvider:DefaultCaskTypeProvider) : DefaultOperationGenerator {
   override fun generate(returnType: Type): QueryOperation {
      val vyneQlType = typeProvider.vyneQlQueryType()
      return QueryOperation(
         "vyneQlQuery${returnType.toQualifiedName().typeName}",
         annotations = listOf(HttpOperations.httpOperation(
            HttpOperations.HttpMethod.POST,
            VyneQlQueryService.REST_ENDPOINT
         )),
         parameters = listOf(
            Parameter(
               annotations = listOf(HttpOperations.requestBody()),
               type = vyneQlType,
               name = "body",
               constraints = emptyList()
            )
         ),
         returnType = ArrayType.of(returnType),
         grammar = VyneQlGrammar.GRAMMAR_NAME,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         capabilities = listOf(
            FilterCapability(
               Operator.values().toList()
            )
         )
      )
   }

}
