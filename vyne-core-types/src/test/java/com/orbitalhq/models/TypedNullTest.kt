package com.orbitalhq.models

import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldNotBeEqual
import org.junit.Test

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
    @Test
    fun `can get nulls along a path that traverses an array`() {
       // root cause of ORB-941
       val schema = TaxiSchema.from("""
          model Person {
            name: Name inherits String
            addresses: Address[]
         }
         model Address {
           street : StreetName inherits String
         }
       """.trimIndent())
       val typedNull = TypedNull.create(schema.type("Person"))
       val nulls = typedNull.nullsForPropertyPath("addresses.street")
       nulls.shouldHaveSize(2)
    }
    @Test
    fun `cached nulls do not incorrectly cache old type definition`() {
       val schema1 = TaxiSchema.from("""
          model Person {
            name: Name inherits String
            addresses: Address[]
         }
         model Address {
           street : StreetName inherits String
         }
       """.trimIndent())
       val typedNull = TypedNull.create(schema1.type("Person"))
       val nulls1 = typedNull.nullsForPropertyPath("addresses.street")
       val schema2 = TaxiSchema.from("""
          model Person {
            name: Name inherits String
            // Address field (not addresses)
            address: Address
         }
         model Address {
           street : StreetName inherits String
         }
       """.trimIndent())
       schema1.type("Person").shouldNotBeEqual(schema2.type("Person"))
       val typedNull2 = TypedNull.create(schema2.type("Person"))
       val nulls = typedNull2.nullsForPropertyPath("address.street")
       nulls.shouldHaveSize(2)
    }
 }
