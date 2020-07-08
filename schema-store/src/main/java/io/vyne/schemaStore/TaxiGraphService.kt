package io.vyne.schemaStore

import io.vyne.query.graph.Element
import io.vyne.query.graph.ElementType
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

data class SchemaGraphNode(val id: String, val label: String, val type: ElementType)
data class SchemaGraphLink(val source: String, val target: String, val label: String)
data class SchemaGraph(val nodes: List<SchemaGraphNode>, val links: List<SchemaGraphLink>)

data class SimpleCompilationError(val line: Int, val char: Int, val message: String?)
@ResponseStatus(HttpStatus.BAD_REQUEST)
data class CompilationExceptionResponse(val errors: List<SimpleCompilationError>)

@ControllerAdvice
class CompilationExceptionControllerAdvice : ResponseEntityExceptionHandler() {

   @ExceptionHandler(CompilationException::class)
   fun handleCompilationException(compilationException: CompilationException, request: WebRequest): ResponseEntity<Any> {
      val response = CompilationExceptionResponse(compilationException.errors.map { SimpleCompilationError(it.line, it.char, it.detailMessage) })
      return handleExceptionInternal(compilationException, response,
         HttpHeaders(), HttpStatus.NOT_ACCEPTABLE, request)
   }
}


