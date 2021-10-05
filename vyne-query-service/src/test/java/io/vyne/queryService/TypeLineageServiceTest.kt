package io.vyne.queryService

import com.winterbe.expekt.should
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.ConsumedOperation
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class TypeLineageServiceTest {
   @Test
   fun `can trace lineage of dataType`() {
      val schema = TaxiSchema.from("""
        type EmailAddress inherits String
        model Customer {
            email : EmailAddress
        }
        model PhoneNumbers {
            phoneNumber : PhoneNumber inherits String
        }
        model AnotherModel {
            emailAddress : EmailAddress
        }
        service ServiceA {
            operation listAllEmails():Customer[]
            operation listPhoneNumbers():PhoneNumbers[]
        }
        service ServiceB {
            lineage {
               consumes operation ServiceA.listAllEmails
               consumes operation ServiceA.listPhoneNumbers
               consumes operation ServiceC.makeACircularLoop
            }
            operation getMoreInfo():AnotherModel[]
        }
        service ServiceC {
            lineage {
               consumes operation ServiceB.getMoreInfo
            }
            operation makeACircularLoop():AnotherModel[]
        }
     """.trimIndent())
      val service = TypeLineageService(SimpleSchemaProvider(schema))
      val lineage = service.getLineageForType("EmailAddress")
      lineage.should.have.size(3)
      lineage[0].should.equal(ServiceLineageForType("ServiceA".fqn(), emptyList()))
      lineage[1].should.equal(ServiceLineageForType("ServiceB".fqn(), listOf(
         ConsumedOperation("ServiceA", "listAllEmails"),
         ConsumedOperation("ServiceC", "makeACircularLoop"),
      )))
      lineage[2].should.equal(ServiceLineageForType("ServiceC".fqn(), listOf(
         ConsumedOperation("ServiceB", "getMoreInfo"),
      )))
   }
}
