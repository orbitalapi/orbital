package io.vyne.cask.rest

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@RunWith(SpringRunner::class)
@WebMvcTest(CaskRestController::class)
class CaskRestControllerTest {
   @Autowired
   lateinit var mockMvc: MockMvc

   @MockBean
   lateinit var service: CaskRestController


   @Test
   fun `fetch cask configs`() {
      whenever(service.getCasks()).thenReturn(listOf())
      mockMvc.perform(get("/api/casks")).andExpect(status().isOk).andExpect(content().json("[]"))
   }

   @Test
   fun `delete by table name`() {
      var tableName: String? = null
      whenever(service.deleteCask(any())).thenAnswer { invocationOnMock ->
         tableName = invocationOnMock.getArgument(0) as String
         Unit
      }

      mockMvc.perform(delete("/api/casks/mytable")).andExpect(status().isOk)
      tableName.should.equal("mytable")
   }

   @Test
   fun `delete by type name`() {
      var typeName: String? = null
      whenever(service.deleteCaskByTypeName(any())).thenAnswer { invocationOnMock ->
         typeName = invocationOnMock.getArgument(0) as String
         Unit
      }

      mockMvc.perform(delete("/api/types/cask/typeName")).andExpect(status().isOk)
      typeName.should.equal("typeName")
   }
}

