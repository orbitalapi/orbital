package io.vyne.pipelines.jet

import com.winterbe.expekt.should
import io.vyne.query.connectors.OperationInvoker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
   classes = [JetPipelineApp::class],
   properties = ["spring.main.allow-bean-definition-overriding=true"]
)
class JetPipelineAppTest {
   @Autowired
   private lateinit var operationInvokers: List<OperationInvoker>

   @Test
   fun startsUpWithRequiredInvokers() {
      operationInvokers.size.should.equal(7)
   }
}
