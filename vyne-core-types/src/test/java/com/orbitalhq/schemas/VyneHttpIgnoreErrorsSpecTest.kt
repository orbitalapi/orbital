package com.orbitalhq.schemas

import com.winterbe.expekt.should
import org.junit.jupiter.api.Test

class VyneHttpIgnoreErrorsSpecTest {
    @Test
    fun `can handle http status code ranges`() {
        val spec = VyneHttpIgnoreErrorsSpec(setOf("4XX","5XX"))
        spec.match(400).should.equal(true)
        spec.match(502).should.equal(true)
        spec.match(302).should.equal(false)
    }

    @Test
    fun `can handle http individual status codes`() {
        val spec = VyneHttpIgnoreErrorsSpec(setOf("422","5XX"))
        spec.match(400).should.equal(false)
        spec.match(502).should.equal(true)
        spec.match(302).should.equal(false)
        spec.match(422).should.equal(true)
    }
}