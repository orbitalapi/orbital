package io.vyne.cask.query

import arrow.core.Either
import io.vyne.cask.CaskService
import io.vyne.cask.query.generators.BetweenVariant
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.http.HttpHeaders
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import java.util.stream.Stream


@Component
class CaskApiHandler(private val caskService: CaskService, private val caskDAO: CaskDAO) {
   fun findBy(request: ServerRequest): Mono<ServerResponse> {

      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      return when {
         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GteLte}") -> findByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GteLte) }


         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GtLte}") -> findByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GtLte) }
         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GtLt}") -> findByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GtLt) }
         uriComponents.pathSegments.contains(OperationAnnotation.Between.annotation) -> findByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String -> caskDAO.findBetween(versionedType, columnName, start, end) }
         uriComponents.pathSegments.contains(OperationAnnotation.After.annotation) -> findByAfter(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.Before.annotation) -> findByBefore(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains("findOneBy") -> findOneBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains("findMultipleBy") -> findMultipleBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains("findSingleBy") -> findSingleBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains("findAll") -> findAll(request, requestPath, uriComponents)
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
            val results = caskDAO.findAll(versionedType.b)
            streamingResponse(request, results)
         }
      }
   }

   private fun findSingleBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val requestPath = requestPathOriginal.replace("findSingleBy/", "")
      return findOne(request, requestPath, uriComponents)
   }

   private fun findMultipleBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      // Example request url => http://192.168.1.114:8800/api/cask/findMultipleBy/ion/trade/Trade/orderId
      val extractor = BodyExtractors.toMono(object : ParameterizedTypeReference<List<String>>() {})
      return request.body(extractor).flatMap { inputArray ->
         val requestPath = requestPathOriginal.replace("findMultipleBy/", "")
         val fieldName = uriComponents.pathSegments.takeLast(1).first()
         val caskType = uriComponents.pathSegments.dropLast(1).drop(1).joinToString(".")
         when (val versionedType = caskService.resolveType(caskType)) {
            is Either.Left -> {
               log().info("The type failed to resolve for request $requestPath Error: ${versionedType.a.message}")
               badRequest().build()
            }
            is Either.Right -> {
               val results = caskDAO.findMultiple(versionedType.b, fieldName, inputArray)
               streamingResponse(request, results)
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
            val results = caskDAO.findBy(versionedType.b, fieldName, findByValue)
            streamingResponse(request, results)
         }
      }
   }


   fun findOneBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val requestPath = requestPathOriginal.replace("findOneBy/", "")
      return findOne(request, requestPath, uriComponents)
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
            val results = caskDAO.findBefore(versionedType.b, fieldName, before)
            streamingResponse(request, results)
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
            val results = caskDAO.findAfter(versionedType.b, fieldName, after)
            streamingResponse(request, results)
         }
      }
   }

   fun findByBetween(request: ServerRequest,
                     requestPath: String,
                     uriComponents: UriComponents,
                     daoFunction: (versionedType: VersionedType, fieldName: String, start: String, end: String) -> Stream<Map<String, Any>>):
      Mono<ServerResponse> {
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
            val results = daoFunction(versionedType.b, fieldName, start, end)
            streamingResponse(request, results)
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

   private fun findOne(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
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
            if (record.isNullOrEmpty()) {
               return notFound().build()
            } else {

               return ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
                  .header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, record.size.toString())
                  .body(BodyInserters.fromValue(record))
            }
         }
      }
   }

   private fun fieldNameAndArgs(uriComponents: UriComponents, takeLast: Int) = uriComponents.pathSegments.map { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }.takeLast(takeLast)

   private fun streamingResponse(request: ServerRequest, results: Stream<Map<String, Any>>):Mono<ServerResponse> {
      if ( request.headers() != null && request.headers().accept() != null && request.headers().accept().any { it == MediaType.TEXT_EVENT_STREAM }
      ){
         return ok()
            .sse()
         //   //.header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, results.size.toString())
            .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
            .body(Flux.fromStream(results))

      } else {

         val resultsAsList = results.collect(Collectors.toList())
         results.close()

         return ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
            .header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, resultsAsList.size.toString())
            .body(BodyInserters.fromValue(resultsAsList))
      }
   }
}
