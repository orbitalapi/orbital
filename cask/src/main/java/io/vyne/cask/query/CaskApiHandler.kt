package io.vyne.cask.query

import arrow.core.Either
import io.vyne.cask.CaskService
import io.vyne.cask.query.generators.BetweenVariant
import io.vyne.cask.query.generators.OperationAnnotation
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.services.QueryMonitor
import io.vyne.http.HttpHeaders
import io.vyne.schemas.VersionedType
import mu.KotlinLogging
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

private val logger = KotlinLogging.logger {}

@Component
class CaskApiHandler(private val caskService: CaskService, private val caskDAO: CaskDAO, private val caskRecordCountDAO: CaskRecordCountDAO,
                     private val queryMonitor: QueryMonitor) {
   fun findBy(request: ServerRequest): Mono<ServerResponse> {

      val requestPath = request.path().replace(CaskServiceSchemaGenerator.CaskApiRootPath, "")
      val uriComponents = UriComponentsBuilder.fromUriString(requestPath).build()
      return when {

         /*
         Streaming Continuous queries
          */
         uriComponents.pathSegments.contains(OperationAnnotation.StreamAll.annotation) -> streamAll(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.StreamAfter.annotation) -> streamByAfter(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.StreamBefore.annotation) -> streamByBefore(request, requestPath, uriComponents)

         uriComponents.pathSegments.contains("${OperationAnnotation.StreamBetween.annotation}${BetweenVariant.GteLte}") -> streamByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.streamBetweenContinuous(versionedType, columnName, start, end, BetweenVariant.GteLte) }

         uriComponents.pathSegments.contains("${OperationAnnotation.StreamBetween.annotation}${BetweenVariant.GtLte}") -> streamByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.streamBetweenContinuous(versionedType, columnName, start, end, BetweenVariant.GtLte) }
         uriComponents.pathSegments.contains("${OperationAnnotation.StreamBetween.annotation}${BetweenVariant.GtLt}") -> streamByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String ->
            caskDAO.streamBetweenContinuous(versionedType, columnName, start, end, BetweenVariant.GtLt) }

         uriComponents.pathSegments.contains("${OperationAnnotation.StreamBetween.annotation}") -> streamByBetween(
            request,
            requestPath,
            uriComponents
         ) { versionedType: VersionedType, columnName: String, start: String, end: String -> caskDAO.streamBetweenContinuous(versionedType, columnName, start, end) }

         /*
         Non Continuous queries
          */

         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GteLte}") -> findByBetween(
            request,
            requestPath,
            uriComponents,
            daoFunction = {versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GteLte)},
            countFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskRecordCountDAO.findCountBetween(versionedType, columnName, start, end, BetweenVariant.GteLte)
            }
         )

         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GtLte}") -> findByBetween(
            request,
            requestPath,
            uriComponents,
            daoFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GtLte) },
            countFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskRecordCountDAO.findCountBetween(versionedType, columnName, start, end, BetweenVariant.GtLte)
            }
         )

         uriComponents.pathSegments.contains("${OperationAnnotation.Between.annotation}${BetweenVariant.GtLt}") -> findByBetween(
            request,
            requestPath,
            uriComponents,
            daoFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskDAO.findBetween(versionedType, columnName, start, end, BetweenVariant.GtLt) },
            countFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskRecordCountDAO.findCountBetween(versionedType, columnName, start, end, BetweenVariant.GtLt)
            }
         )

         uriComponents.pathSegments.contains(OperationAnnotation.Between.annotation) -> findByBetween(
            request,
            requestPath,
            uriComponents,
            daoFunction = { versionedType: VersionedType, columnName: String, start: String, end: String -> caskDAO.findBetween(versionedType, columnName, start, end) },
            countFunction = { versionedType: VersionedType, columnName: String, start: String, end: String ->
               caskRecordCountDAO.findCountBetween(versionedType, columnName, start, end)
            }
         )

         uriComponents.pathSegments.contains(OperationAnnotation.After.annotation) -> findByAfter(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.Before.annotation) -> findByBefore(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindOne.annotation) -> findOneBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindMultipleBy.annotation) -> findMultipleBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindSingleBy.annotation) -> findSingleBy(request, requestPath, uriComponents)
         uriComponents.pathSegments.contains(OperationAnnotation.FindAll.annotation) -> findAll(request, requestPath, uriComponents)

         else -> findByField(request, requestPath, uriComponents)
      }
   }

   fun findAll(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val caskType = uriComponents.pathSegments.drop(1).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.findAll(versionedType.b)
            val resultCount = caskRecordCountDAO.findCountAll(versionedType.b)
            streamingResponse(request, results, resultCount)
         }
      }
   }



   private fun findSingleBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val requestPath = requestPathOriginal.replace("findSingleBy/", "")
      return findOne(request, requestPath, uriComponents)
   }

   private fun findMultipleBy(request: ServerRequest, requestPathOriginal: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val extractor = BodyExtractors.toMono(object : ParameterizedTypeReference<List<String>>() {})
      return request.body(extractor).flatMap { inputArray ->
         val requestPath = requestPathOriginal.replace("findMultipleBy/", "")
         val fieldName = uriComponents.pathSegments.takeLast(1).first()
         val caskType = uriComponents.pathSegments.dropLast(1).drop(1).joinToString(".")
         when (val versionedType = caskService.resolveType(caskType)) {
            is Either.Left -> {
               logger.warn {"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}" }
               badRequest().build()
            }
            is Either.Right -> {
               val results = caskDAO.findMultiple(versionedType.b, fieldName, inputArray)
               val resultCount = caskRecordCountDAO.findCountMultiple(versionedType.b, fieldName, inputArray)
               streamingResponse(request, results, resultCount)
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
            logger.warn{ "The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.findBy(versionedType.b, fieldName, findByValue)
            val resultCount = caskRecordCountDAO.findCountBy(versionedType.b, fieldName, findByValue)
            streamingResponse(request, results, resultCount)
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
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.findBefore(versionedType.b, fieldName, before)
            val resultCount = caskRecordCountDAO.findCountBefore(versionedType.b, fieldName, before)
            streamingResponse(request, results, resultCount)
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
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.findAfter(versionedType.b, fieldName, after)
            val resultCount = caskRecordCountDAO.findCountAfter(versionedType.b, fieldName, after)
            streamingResponse(request, results, resultCount)
         }
      }
   }

   fun findByBetween(request: ServerRequest,
                     requestPath: String,
                     uriComponents: UriComponents,
                     daoFunction: (versionedType: VersionedType, fieldName: String, start: String, end: String) -> Stream<Map<String, Any>>,
                     countFunction: (versionedType: VersionedType, fieldName: String, start: String, end: String) -> Int,
                     ):
      Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 4)
      val fieldName = fieldNameAndValues.first()
      val start = fieldNameAndValues[2]
      val end = fieldNameAndValues[3]
      val caskType = uriComponents.pathSegments.dropLast(4).joinToString(".")

      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.info{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = daoFunction(versionedType.b, fieldName, start, end)
            val resultCount = countFunction(versionedType.b, fieldName, start, end)
            streamingResponse(request, results, resultCount)
         }
      }
   }

   fun streamByBetween(request: ServerRequest,
                     requestPath: String,
                     uriComponents: UriComponents,
                     daoFunction: (versionedType: VersionedType, fieldName: String, start: String, end: String) -> Flux<Map<String, Any>>):
      Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 4)
      val fieldName = fieldNameAndValues.first()
      val start = fieldNameAndValues[2]
      val end = fieldNameAndValues[3]
      val caskType = uriComponents.pathSegments.dropLast(4).joinToString(".")

      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = daoFunction(versionedType.b, fieldName, start, end)
            continuousResponse(results)
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
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
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

   fun streamAll(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val caskType = uriComponents.pathSegments.drop(1).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            continuousResponse(caskDAO.streamAll(versionedType.b))
         }
      }
   }

   fun streamByAfter(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 3)
      val fieldName = fieldNameAndValues.first()
      val after = fieldNameAndValues[2]
      val caskType = uriComponents.pathSegments.dropLast(3).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.steamAfterContinuous(versionedType.b, fieldName, after)
            continuousResponse(results)
         }
      }
   }

   fun streamByBefore(request: ServerRequest, requestPath: String, uriComponents: UriComponents): Mono<ServerResponse> {
      val fieldNameAndValues = fieldNameAndArgs(uriComponents, 3)
      val fieldName = fieldNameAndValues.first()
      val before = fieldNameAndValues[2]
      val caskType = uriComponents.pathSegments.dropLast(3).joinToString(".")
      return when (val versionedType = caskService.resolveType(caskType)) {
         is Either.Left -> {
            logger.warn{"The type failed to resolve for request $requestPath Error: ${versionedType.a.message}"}
            badRequest().build()
         }
         is Either.Right -> {
            val results = caskDAO.streamBeforeContinuous(versionedType.b, fieldName, before)
            continuousResponse(results)
         }
      }
   }

   private fun fieldNameAndArgs(uriComponents: UriComponents, takeLast: Int) = uriComponents.pathSegments.map { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }.takeLast(takeLast)

   private fun streamingResponse(request: ServerRequest, results: Stream<Map<String, Any>>, resultCount: Int):Mono<ServerResponse> {
      if ( request.headers() != null && request.headers().accept() != null && request.headers().accept().any { it == MediaType.TEXT_EVENT_STREAM }
      ){

         return ok()
            .sse()
            .header(HttpHeaders.STREAM_ESTIMATED_RECORD_COUNT, resultCount.toString())
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

   private fun continuousResponse(results: Flux<Map<String, Any>>):Mono<ServerResponse> {

         return ok()
            .sse()
            .header(HttpHeaders.CONTENT_PREPARSED, true.toString())
            .body(results)

   }

}
