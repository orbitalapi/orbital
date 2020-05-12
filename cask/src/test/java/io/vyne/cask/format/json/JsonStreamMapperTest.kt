package io.vyne.cask.format.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.Resources
import io.vyne.VersionedTypeReference
import io.vyne.cask.format.csv.Benchmark
import io.vyne.utils.log
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
            val stream = JsonStreamSource(Flux.just(File(resource).inputStream()), versionedType, schema, folder.root.toPath(), ObjectMapper())
            val noOfMappedRows = stream
                    .stream
                    .count()
                    .block()

            log().info("Mapped ${noOfMappedRows} rows to typed instance")
        }
    }
}
