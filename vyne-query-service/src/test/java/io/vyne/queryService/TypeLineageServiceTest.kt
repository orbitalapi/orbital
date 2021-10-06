package io.vyne.queryService

import com.winterbe.expekt.should
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.ConsumedOperation
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class TypeLineageServiceTest {
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

   @Test
   fun `can trace lineage of dataType`() {
      val typeLineageService = TypeLineageService(SimpleSchemaProvider(schema))
      val lineage = typeLineageService.getLineageForType("EmailAddress")
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

   @Test // Bug -- why isn't this working?
   fun `can trace lineage of phone number`() {
      val typeLineageService = TypeLineageService(SimpleSchemaProvider(schema))
      val lineage = typeLineageService.getLineageForType("PhoneNumber")
      lineage.should.have.size(2)
      lineage[1].serviceName.fullyQualifiedName.should.equal("ServiceB")
      lineage[1].consumesVia.single().operationQualifiedName.fullyQualifiedName.should.equal("ServiceA@@listPhoneNumbers")
   }

//   @Test
//   fun `can trace lineage of a service`() {
//      val typeLineageService = TypeLineageService(SimpleSchemaProvider(schema))
//      val lineage = typeLineageService.getLineageGraphForService("ServiceB")
//      TODO()
//   }
//   @Test
//   fun `service lineage shows inbound links`() {
//      val typeLineageService = TypeLineageService(SimpleSchemaProvider(schema))
//      val lineage = typeLineageService.getLineageGraphForService("ServiceA")
//      TODO()
//   }
}
