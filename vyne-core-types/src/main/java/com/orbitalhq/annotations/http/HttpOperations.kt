package com.orbitalhq.annotations.http

import lang.taxi.types.Annotation

object HttpOperations {

   enum class HttpMethod {
      GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE
   }

   // TODO : Replace these from returning Annotation to returning AnnotationType, so
   // we're using strongly typed annotations throughout code
   fun requestBody():Annotation {
      return Annotation("RequestBody")
   }
}
