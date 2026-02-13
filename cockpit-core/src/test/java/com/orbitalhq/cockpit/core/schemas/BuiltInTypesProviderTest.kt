package com.orbitalhq.cockpit.core.schemas

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BuiltInTypesProviderTest : DescribeSpec({

   describe("compiling built-in types") {
      it("should compile built-in types without errors") {
         val schema = BuiltInTypesProvider.asTaxiSchema()

         // assert some of the more complex things are there
         val cachePolicy = schema.type("com.orbitalhq.caching.CachePolicy")
         val cacheAnnotation = schema.type("com.orbitalhq.caching.Cache")
      }
   }


})
