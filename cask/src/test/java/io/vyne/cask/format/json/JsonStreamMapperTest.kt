package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.cask.MessageIds
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Flux
import java.io.File

class JsonStreamMapperTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun can_ingestAndMapToTypedInstance() {
      val schema = CoinbaseJsonOrderSchema.schemaV1
      val typeReference = "OrderWindowSummary"
      val versionedType = schema.versionedType(VersionedTypeReference.parse(typeReference))
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      // Ingest it a few times to get an average performance
      Benchmark.benchmark("can_ingestAndMapToTypedInstance") {
         val stream = JsonStreamSource(Flux.just(File(resource).inputStream()), versionedType, schema, MessageIds.uniqueId(), ObjectMapper())
         val noOfMappedRows = stream
            .stream
            .count()
            .block()

         log().info("Mapped ${noOfMappedRows} rows to typed instance")
      }
   }

   @Test
   fun `can ingest for a schema with nullable fields`() {
      val taxiSchema = """
         type ModelWithNullableFields {
             stringNullable: String? by jsonPath("${'$'}.string")
             intNullable: Int? by jsonPath("${'$'}.int")
             dateNullable: Date? by jsonPath("${'$'}.date")
             id: String by jsonPath("${'$'}.id")
         }
      """.trimIndent()
      val schema = TaxiSchema.from(taxiSchema, "Nullable", "0.1.0")
      val typeReference = "ModelWithNullableFields"
      val versionedType = schema.versionedType(VersionedTypeReference.parse(typeReference))
      val inputStream = IOUtils.toInputStream("""
         {  "id": "1" }
      """.trimIndent())

      val instanceAttributeSet = JsonStreamSource(Flux.just(inputStream), versionedType, schema, MessageIds.uniqueId(), ObjectMapper())
         .stream
         .collectList()
         .block()
         .first()

      instanceAttributeSet.attributes["id"]?.value.should.not.be.`null`
      instanceAttributeSet.attributes["stringNullable"]?.value.should.be.`null`
      instanceAttributeSet.attributes["intNullable"]?.value.should.be.`null`
      instanceAttributeSet.attributes["dateNullable"]?.value.should.be.`null`


   }
}
