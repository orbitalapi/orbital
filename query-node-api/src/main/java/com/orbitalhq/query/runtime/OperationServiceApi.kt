package com.orbitalhq.query.runtime

import com.orbitalhq.query.Fact
import com.orbitalhq.query.ResultMode
import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

interface OperationServiceApi {
   @PostMapping("/api/services/{serviceName}/{operationName}")
   suspend fun invokeOperation(
      @PathVariable("serviceName") serviceName: String,
      @PathVariable("operationName") operationName: String,
      @RequestParam("resultMode", defaultValue = "RAW") resultMode: ResultMode,
      @RequestBody facts: Map<String, Fact>
   ): ResponseEntity<Flow<Any>>
}
