package io.vyne.http

// This is copied from here:
// https://raw.githubusercontent.com/joansmith1/okhttp/master/mockwebserver/src/main/java/com/squareup/okhttp/mockwebserver/rule/MockWebServerRule.java
// And adapted to work with MockWebserver 3

/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource
import org.springframework.http.MediaType
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

private val logger = KotlinLogging.logger {}

/**
 * Allows you to use [MockWebServer] as a JUnit test rule.
 *
 *
 * This rule starts [MockWebServer] on an available port before your test runs, and shuts
 * it down after it completes.
 */
class MockWebServerRule : ExternalResource() {
   private val server = MockWebServer()
   private var started = false
   override fun before() {
      if (started) return
      started = true
      try {
         server.start()
      } catch (e: IOException) {
         throw RuntimeException(e)
      }
   }

   override fun after() {
      try {
         server.shutdown()
      } catch (e: IOException) {
         logger.log(Level.WARNING, "MockWebServer shutdown failed", e)
      }
   }

   val hostName: String
      get() {
         if (!started) before()
         return server.hostName
      }
   val port: Int
      get() {
         if (!started) before()
         return server.port
      }
   val requestCount: Int
      get() = server.requestCount

   fun enqueue(response: MockResponse?) {
      server.enqueue(response!!)
   }

   @Throws(InterruptedException::class)
   fun takeRequest(timeoutInSeconds: Long = 10L): RecordedRequest {
      return server.takeRequest(timeoutInSeconds, TimeUnit.SECONDS)!!
   }


   fun url(path: String?): HttpUrl {
      return server.url(path!!)
   }

   /** For any other functionality, use the [MockWebServer] directly.  */
   fun get(): MockWebServer {
      return server
   }

   fun prepareResponse(consumer: Consumer<MockResponse>) {
      server.prepareResponse(consumer)
   }

   fun addJsonResponse(json: String) {
      server.prepareResponse { response ->
         response
            .setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody(json)
      }
   }

   fun prepareResponse(
      recordInvokedPathsTo: ConcurrentHashMap<String, Int>,
      vararg responses: Pair<String, (String) -> MockResponse>
   ) {
      server.prepareResponse(recordInvokedPathsTo, *responses)
   }

   companion object {
      private val logger = Logger.getLogger(MockWebServerRule::class.java.name)
   }
}

fun MockWebServer.prepareResponse(consumer: Consumer<MockResponse>) {
   val response = MockResponse()
   consumer.accept(response)
   this.enqueue(response)
}

fun MockWebServer.prepareResponse(
   recordInvokedPathsTo: ConcurrentHashMap<String, Int>,
   vararg responses: Pair<String, (String) -> MockResponse>
) {
   this.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
         logger.info { "Received call to ${request.path}" }
         recordInvokedPathsTo.compute(request.path!!) { key, value ->
            if (value == null) 1 else value + 1
         }
         val handler =
            responses.firstOrNull { request.path!!.startsWith(it.first) }
               ?: error("No handler for path ${request.path}")
         return handler.second.invoke(request.path!!)
      }

   }
}

fun respondWith(
   responseCode: Int = 200,
   contentType: MediaType = MediaType.APPLICATION_JSON,
   bodyFn: (String) -> String
): (String) -> MockResponse {
   return { path ->
      MockResponse().setHeader("Content-Type", contentType).setBody(bodyFn(path))
         .setResponseCode(responseCode)
   }
}

fun response(
   body: String,
   responseCode: Int = 200,
   contentType: MediaType = MediaType.APPLICATION_JSON
): (String) -> MockResponse {
   return respondWith(responseCode, contentType) { _ -> body }
}
