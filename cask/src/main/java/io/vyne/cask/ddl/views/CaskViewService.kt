package io.vyne.cask.ddl.views

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.utils.log
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class CaskViewService(val viewBuilderFactory: CaskViewBuilderFactory,
                      val caskConfigRepository: CaskConfigRepository,
                      val template: JdbcTemplate,
                      val viewConfig: CaskViewConfig) {
   private fun generateView(viewDefinition: CaskViewDefinition): CaskConfig? {
      log().info("Generating view ${viewDefinition.typeName}")
      val builder = viewBuilderFactory.getBuilder(viewDefinition)
      val viewDdl = builder.generateCreateView()
      log().info("View ${viewDefinition.typeName} view using DDL: $viewDdl \n")
      template.execute(viewDdl)
      log().info("View ${viewDefinition.typeName} created successfully")

      log().info("Generating cask config for view ${viewDefinition.typeName}")
      val caskConfig = builder.generateCaskConfig()
      if (!caskConfigRepository.findById(caskConfig.tableName).isPresent) {
         return caskConfigRepository.save(caskConfig)
      }
      log().info("Cask Config for view ${viewDefinition.typeName} created successfully")
      return null

   }

   fun bootstrap(): List<CaskConfig> {
      return viewConfig.views.mapNotNull { generateView(it) }
   }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "cask")
data class CaskViewConfig(
   val views: List<CaskViewDefinition> = emptyList()
)

