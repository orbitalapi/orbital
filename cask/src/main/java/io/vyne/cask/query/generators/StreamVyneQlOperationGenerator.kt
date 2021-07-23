package io.vyne.cask.query.generators

import io.vyne.annotations.http.HttpOperations
import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.query.vyneql.VyneQLContinuousQueryService
import io.vyne.cask.services.DefaultCaskTypeProvider
import io.vyne.query.queryBuilders.VyneQlGrammar
import lang.taxi.Operator
import lang.taxi.services.FilterCapability
import lang.taxi.services.Parameter
import lang.taxi.services.QueryOperation
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Type
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * OperationGenerator to generate operation for stream queries to be handled by vyne QL e.g.
 * stream { some.thing (attribute='val') }
 */
@Component
@ConditionalOnProperty("cask.streamQueries.enabled", havingValue = "true")
class StreamVyneQlOperationGenerator(private val typeProvider:DefaultCaskTypeProvider) : DefaultOperationGenerator {
   override fun generate(returnType: Type): QueryOperation {
      val vyneQlType = typeProvider.vyneQlQueryType()
      return QueryOperation(
         "streamVyneQlQuery${returnType.toQualifiedName().typeName}",
         annotations = listOf(HttpOperations.httpOperation(
            HttpOperations.HttpMethod.POST,
            VyneQLContinuousQueryService.REST_CONTINUOUS_QUERY
         )),
         parameters = listOf(
            Parameter(
               annotations = listOf(HttpOperations.requestBody()),
               type = vyneQlType,
               name = "body",
               constraints = emptyList()
            )
         ),
         returnType = TemporalFieldUtils.streamTypeOf(returnType),
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
