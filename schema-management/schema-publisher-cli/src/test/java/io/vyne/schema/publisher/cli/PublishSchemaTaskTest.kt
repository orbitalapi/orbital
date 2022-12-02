package io.vyne.schema.publisher.cli

import com.google.common.io.Resources
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.net.URI
import java.nio.file.Paths

class PublishSchemaTaskTest : DescribeSpec({

   // Used this test whilst spiking.  Need to add real tests when we promote this thing.
   xit("should publish the spec to a schema server") {
      val serverUrl = URI.create("http://localhost:9305")
      val conf = Paths.get(Resources.getResource("sample-spec/taxi.conf").toURI())
      val spec = Paths.get(Resources.getResource("sample-spec/oas.yaml").toURI())

      val publicationTask = PublishSchemaTask(spec, serverUrl, conf)
      publicationTask.run()
   }

})
