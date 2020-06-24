package io.vyne.cask.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import org.junit.Test

class CaskIngestionResponseTest {

    @Test
    fun testSerializationAndDeserialization() {
        val mapper = jacksonObjectMapper()
        val response = CaskIngestionResponse.success("Successful ingestion")
        val jsonMessage = """{"result":"SUCCESS","message":"Successful ingestion"}"""

        mapper.writeValueAsString(response).should.equal(jsonMessage)
        mapper.readValue(jsonMessage, CaskIngestionResponse::class.java).should.equal(response)
    }
}
