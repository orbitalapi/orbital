package com.orbitalhq.schemaServer.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeTypeOf
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec

class CreateFileRepositoryRequestTest : DescribeSpec({

   it("can deserialize with Taxi spec") {
      val mapper = jacksonObjectMapper()
      val request =
         mapper.readValue<CreateFileRepositoryRequest>("""{"loader":{"packageType":"Taxi"},"isEditable":true,"path":"/home/martypitt/dev/vyne-demos/films/taxi"}""")
      request.shouldNotBeNull()
      request.loader.shouldBeTypeOf<TaxiPackageLoaderSpec>()
   }

   it("can deserialize with OpenAPI spec") {
      val mapper = jacksonObjectMapper()
      val request =
         mapper.readValue<CreateFileRepositoryRequest>(
            """{
  "loader": {
    "packageType": "OpenApi",
    "identifier": {
      "name": "petstore",
      "organisation": "orbital",
      "version": "1.0.0",
      "id": null,
      "unversionedId": null
    },
    "defaultNamespace": "orbital.petstore"
  },
  "isEditable": true,
  "newProjectIdentifier": null,
  "path": "/home/martypitt/dev/vyne/schema-management/schema-publisher-cli/src/test/resources/sample-spec/oas.yaml"
}"""
         )
      request.shouldNotBeNull()
      request.loader.shouldBeEqualToIgnoringFields(
         OpenApiPackageLoaderSpec(
            PackageIdentifier.fromId("orbital/petstore/1.0.0"),
            null,
            "orbital.petstore",
         ),
         property = OpenApiPackageLoaderSpec::submissionDate
      )
   }
})
