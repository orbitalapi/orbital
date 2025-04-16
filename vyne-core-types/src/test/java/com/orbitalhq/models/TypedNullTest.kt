package com.orbitalhq.models

import com.orbitalhq.from
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
 class TypedNullTest {
    @Test
    fun `can get nulls along a path`() {
       // root cause of ORB-941
       val schema = TaxiSchema.from("""
          model Person {
            name: Name inherits String
            address: Address
         }
         model Address {
           street : StreetName inherits String
         }
       """.trimIndent())
       val typedNull = TypedNull.create(schema.type("Person"))
       val nulls = typedNull.nullsForPropertyPath("address.street")
       nulls.shouldHaveSize(2)
    }
 }
