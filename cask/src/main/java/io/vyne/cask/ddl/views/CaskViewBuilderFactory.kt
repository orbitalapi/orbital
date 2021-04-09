package io.vyne.cask.ddl.views

import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.StringToQualifiedNameConverter
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemaStore.SchemaStore
import org.springframework.stereotype.Component

@Component
class CaskViewBuilderFactory(private val caskConfigRepository: CaskConfigRepository,
                             private val schemaStore: SchemaStore,
                             // Though this is not used, please don't remove as Spring can't handle
                             // the correct initialisation order for CaskViewConfig configuration property
                             // without this.
                             val stringToQualifiedNameConverter: StringToQualifiedNameConverter) {


   fun getBuilder(viewDefinition: CaskViewDefinition):CaskViewBuilder {
      return CaskViewBuilder(caskConfigRepository,schemaStore,viewDefinition)
   }
}
