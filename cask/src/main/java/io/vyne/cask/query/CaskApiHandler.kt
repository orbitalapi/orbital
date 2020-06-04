package io.vyne.cask.query

import arrow.core.Either
import io.vyne.cask.CaskService
import io.vyne.utils.log
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@Component
class CaskApiHandler(private val caskService: CaskService, private val caskDAO: CaskDAO) {
   fun findBy(request: ServerRequest): Mono<ServerResponse> {
      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      if (uriComponents.pathSegments.contains("Between")) {
         //TODO Rewrite this if a field name is called Between, this will blow up.
         return findByBetween(request)
      }
      val fieldNameAndValue = uriComponents.pathSegments.takeLast(2)
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

   fun findByBetween(request: ServerRequest): Mono<ServerResponse> {
      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      val fieldNameAndValues = uriComponents.pathSegments.takeLast(4)
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
}
