package io.vyne.cask.ddl.views

import io.vyne.cask.config.CaskConfigRepository
import io.vyne.schemaStore.SchemaStore
import org.springframework.stereotype.Component

@Component
class CaskViewBuilderFactory(private val caskConfigRepository: CaskConfigRepository,
                             private val schemaStore: SchemaStore) {


   fun getBuilder(viewDefinition: CaskViewDefinition):CaskViewBuilder {
      return CaskViewBuilder(caskConfigRepository,schemaStore,viewDefinition)
   }
}
