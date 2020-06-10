package io.vyne.cask.query

import arrow.core.Either
import io.vyne.cask.CaskService
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.query.generators.AfterTemporalOperationGenerator
import io.vyne.cask.query.generators.BeforeTemporalOperationGenerator
import io.vyne.cask.query.generators.BetweenTemporalOperationGenerator
import io.vyne.utils.log
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@Component
class CaskApiHandler(private val caskService: CaskService, private val caskDAO: CaskDAO) {
   fun findBy(request: ServerRequest): Mono<ServerResponse> {
      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      return when {
         uriComponents.pathSegments.contains(BetweenTemporalOperationGenerator.ExpectedAnnotationName) -> findByBetween(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(AfterTemporalOperationGenerator.ExpectedAnnotationName) -> findByAfter(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(BeforeTemporalOperationGenerator.ExpectedAnnotationName) -> findByBefore(request, requestPath, uriComponents)
         else -> findByField(request, requestPath, uriComponents)
      }
   }

   fun findByField(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValue = fieldNameAndArgs(uriComponents, 2)
      val fieldName = fieldNameAndValue.first()
      val findByValue = fieldNameAndValue.last()
      val caskType = uriComponents.pathSegments.dropLast(2).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            ok()
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(caskDAO.findBy(versionedType.b, fieldName, findByValue)))
         }
      }
   }

   fun findByBefore(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 3)
      val fieldName = fieldNameAndValues.first()
      val before = fieldNameAndValues[2]
      val caskType = uriComponents.pathSegments.dropLast(3).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            ok()
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(caskDAO.findBefore(versionedType.b, fieldName, before)))
         }
      }
   }

   fun findByAfter(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 3)
      val fieldName = fieldNameAndValues.first()
      val after = fieldNameAndValues[2]
      val caskType = uriComponents.pathSegments.dropLast(3).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            ok()
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(caskDAO.findAfter(versionedType.b, fieldName, after)))
         }
      }
   }

   fun findByBetween(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 4)
      val fieldName = fieldNameAndValues.first()
      val start = fieldNameAndValues[2]
      val end = fieldNameAndValues[3]
      val caskType = uriComponents.pathSegments.dropLast(4).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            ok()
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(caskDAO.findBetween(versionedType.b, fieldName, start, end)))
         }
      }
   }

   private fun fieldNameAndArgs(uriComponents: UriComponents, takeLast: Int ) = uriComponents.pathSegments.takeLast(takeLast)
}
