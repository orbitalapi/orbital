package io.vyne.cask.rest

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.config.JdbcStreamingTemplate
import io.vyne.cask.query.CaskApiHandler
import io.vyne.cask.query.vyneql.VyneQlSqlGenerator
import io.vyne.cask.websocket.CaskWebsocketHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import reactor.core.publisher.Mono
import org.springframework.test.web.reactive.server.WebTestClient





@RunWith(SpringRunner::class)
@WebFluxTest
class CaskRestControllerTest {

   @Autowired
   lateinit var client: WebTestClient

   @MockBean
   lateinit var service: CaskRestController

   @MockBean
   lateinit var jdbcStreamingTemplate:JdbcStreamingTemplate

   @MockBean
   lateinit var vyneQlSqlGenerator: VyneQlSqlGenerator

   @MockBean
   lateinit var caskWebsocketHandler: CaskWebsocketHandler

   @MockBean
   lateinit var caskApiHandler: CaskApiHandler


   @Test
   fun `fetch cask configs`() {
      whenever(service.getCasks()).thenReturn(Mono.just(listOf()))
      client.get().uri("/api/casks").exchange().expectStatus().isOk().expectBody( ArrayList::class.java )
   }


   @Test
   fun `delete by table name`() {
      var tableName: String? = null
      var force: Boolean? = null
      whenever(service.deleteCask(any(), any())).thenAnswer { invocationOnMock ->
         tableName = invocationOnMock.getArgument(0, String::class.java)
         force = invocationOnMock.getArgument(1) as Boolean
         Mono.just(tableName)
      }

      client.delete().uri("/api/casks/mytable").exchange().expectStatus().isOk()
      tableName.should.equal("mytable")
      force.should.be.`false`
   }


   @Test
   fun `delete by type name`() {
      var typeName: String? = null
      var force: Boolean? = null
      whenever(service.deleteCaskByTypeName(any(), any())).thenAnswer { invocationOnMock ->
         typeName = invocationOnMock.getArgument(0) as String
         force = invocationOnMock.getArgument(1) as Boolean
         Mono.just(typeName)
      }

      client.delete().uri("/api/types/cask/typeName").exchange().expectStatus().isOk()
      typeName.should.equal("typeName")
      force.should.be.`false`
   }


}

