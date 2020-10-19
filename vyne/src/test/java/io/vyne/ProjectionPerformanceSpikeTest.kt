package io.vyne

import io.vyne.cask.api.CsvIngestionParameters
import io.vyne.models.TypedCollection
import io.vyne.models.csv.CsvImporterUtil
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Paths

@Ignore("This test requires access to client specific content, which is not checked in.  Keeping the test here, as its useful for spiking perf improvements.")
class ProjectionPerformanceSpikeTest {

   @Test
   fun investigate() {
      val schema = TaxiSchema.forPackageAtPath(Paths.get("/home/marty/dev/cacib/taxonomy"))
      val (vyne, stub) = testVyne(schema)
      val csv = Paths.get("/home/marty/Documents/demo-files/all_sb2.csv").toFile().readText()
      val orders = TypedCollection.from(CsvImporterUtil.parseCsvToType(csv, CsvIngestionParameters(), schema, "bgc.orders.Order")
         .map { it.instance })
      Benchmark.benchmark("Projecting orders", warmup = 5, iterations = 10) {
         vyne.from(orders).build("cacib.imad.Order[]")
      }
   }
}
