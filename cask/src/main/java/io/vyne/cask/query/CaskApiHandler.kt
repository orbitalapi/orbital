package io.vyne.cask.query

import arrow.core.Either
import io.vyne.cask.CaskService
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.utils.log
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Component
class CaskApiHandler(private val caskService: CaskService, private val caskDAO: CaskDAO) {
   fun findBy(request: ServerRequest): Mono<ServerResponse> {
      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      return when {
         uriComponents.pathSegments.contains(OperationAnnotation.Between.name) -> findByBetween(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.After.name) -> findByAfter(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.Before.name) -> findByBefore(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindMany.annotation) -> findManyBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindOne.annotation) -> findOneBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindAll.annotation) -> findAll(request, requestPath, uriComponents)
         else -> findByField(request, requestPath, uriComponents)
      }
   }

   fun findAll(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val caskType = uriComponents.pathSegments.drop(1).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            val record = caskDAO.findAll(versionedType.b)
            return ok()
               .contentType(MediaType.APPLICATION_JSON)
               .body(BodyInserters.fromValue(record))
         }
      }
   }

   private fun findOneBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val requestPath = requestPathOriginal.replace("${OperationAnnotation.FindOne.annotation}/", "")
      val fieldNameAndValue = fieldNameAndArgs(uriComponents, 2)
      val fieldName = fieldNameAndValue.first()
      val findByValue = decode(fieldNameAndValue.last())
      val caskType = uriComponents.pathSegments.dropLast(2).drop(1).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
            badRequest().build()
         }
         is Either.Right -> {
            val record = caskDAO.findOne(versionedType.b, fieldName, findByValue)
            return if (record.isNullOrEmpty()) {
               notFound().build()
            } else {
               ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BodyInserters.fromValue(record))
            }
         }
      }
   }

   private fun findManyBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      // Example request url => http://192.168.1.114:8800/api/cask/findManyBy/ion/trade/Trade/orderId
      val extractor = BodyExtractors.toMono(object : ParameterizedTypeReference<List<String>>() {})
      return request.body(extractor).flatMap { inputArray ->
         val requestPath = requestPathOriginal.replace("${OperationAnnotation.FindMany.annotation}/", "")
         val fieldName = uriComponents.pathSegments.takeLast(1).first()
         val caskType = uriComponents.pathSegments.dropLast(1).drop(1).joinToString(".")
         when (val versionedType = caskService.resolveType(caskType)) {
            is Either.Left -> {
               log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
               badRequest().build()
            }
            is Either.Right -> {
               ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(BodyInserters.fromValue(caskDAO.findMany(versionedType.b, fieldName, inputArray)))
            }
         }
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

   private fun decode(value: String): String {
      return try {
         URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
      } catch (e: Exception) {
         value
      }
   }

   private fun fieldNameAndArgs(uriComponents: UriComponents, takeLast: Int) = uriComponents.pathSegments.takeLast(takeLast)
}
