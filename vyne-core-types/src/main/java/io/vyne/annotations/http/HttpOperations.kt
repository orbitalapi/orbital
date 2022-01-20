package io.vyne.annotations.http

import lang.taxi.types.Annotation

object HttpOperations {
   const val HTTP_OPERATION_NAME = "HttpOperation"

   enum class HttpMethod {
      GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
   }

   // TODO : Replace these from returning Annotation to returning AnnotationType, so
   // we're using strongly typed annotations throughout code
   fun requestBody():Annotation {
      return Annotation("RequestBody")
   }
   // TODO : Replace these from returning Annotation to returning AnnotationType, so
   // we're using strongly typed annotations throughout code
   fun pathVariable(name: String): Annotation {
      return Annotation("PathVariable",
         mapOf("name" to name)
      )
   }


   // TODO : Replace these from returning Annotation to returning AnnotationType, so
   // we're using strongly typed annotations throughout code
   fun httpOperation(method: HttpMethod, url: String): Annotation {
      return Annotation(
         HTTP_OPERATION_NAME,
         mapOf(
            "method" to method.name,
            "url" to url
         )
      )
   }
}
