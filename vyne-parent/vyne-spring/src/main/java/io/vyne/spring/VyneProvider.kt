package io.vyne.spring

import io.vyne.Vyne
import io.vyne.query.QueryEngineFactory
import io.vyne.query.graph.operationInvocation.OperationInvoker
import io.vyne.schemaStore.SchemaSourceProvider
import org.springframework.beans.factory.FactoryBean


// To make testing easier
interface VyneProvider {
   fun createVyne(): Vyne
}

// To make testing easier
class SimpleVyneProvider(private val vyne: Vyne) : VyneProvider {
   override fun createVyne(): Vyne {
      return vyne
   }

}

class VyneFactory(private val schemaProvider: SchemaSourceProvider, private val operationInvokers: List<OperationInvoker>) : FactoryBean<Vyne>, VyneProvider {
   override fun isSingleton() = true
   override fun getObjectType() = Vyne::class.java

   override fun getObject(): Vyne {
      return buildVyne()
   }

   // For readability
   override fun createVyne() = getObject()

   private fun buildVyne(): Vyne {
      val vyne = Vyne(QueryEngineFactory.withOperationInvokers(operationInvokers))
      vyne.addSchema(schemaProvider.schema())
//      schemaProvider.schemaStrings().forEach { schema ->
//         // TODO :  This is all a bit much ... going to a TaxiSchema and back again.
//         // Should really be able to do:  Vyne().addSchema(TypeSchema.from(type))
//         log().debug("Registering schema: $schema")
//         vyne.addSchema(TaxiSchema.from(schema))
//      }
      return vyne
   }
}
