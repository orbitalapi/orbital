package io.vyne.queryService

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.query.ResultMode
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType

class QueryHistoryServiceTest : BaseQueryServiceTest() {

   lateinit var queryHistoryService: QueryHistoryService

   @Before
   override fun setup() {
      super.setup()
      this.queryHistoryService = QueryHistoryService(queryHistory, mock {}, mock {})
   }

   @Test
   fun canRetrieveNodeDetailsFromHistoryRecord() {
      val queryType =  "Order[]".fqn().parameterizedName
      val query = buildQuery(queryType)
      val responseStr = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
         .contentString()
      val response = jacksonObjectMapper().readValue<Map<String, Any>>(responseStr)
      val queryId = response["queryResponseId"] as String
      val nodeId = "[0].orderId"
      val node = queryHistoryService.getNodeDetail(queryId,queryType,nodeId).block()!!
//      queryHistoryService.getNodeDetail(queryId, )
      node.typeName.should.equal("OrderId".fqn())
      node.source.should.equal(Provided)
      node.attributeName.should.equal("orderId")
   }
}
