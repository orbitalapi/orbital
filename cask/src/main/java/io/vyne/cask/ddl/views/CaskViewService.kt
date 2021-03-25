package io.vyne.cask.ddl.views

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.utils.log
import lang.taxi.types.QualifiedName
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class CaskViewService(val viewBuilderFactory: CaskViewBuilderFactory,
                      val caskConfigRepository: CaskConfigRepository,
                      val template: JdbcTemplate,
                      val viewConfig: CaskViewConfig,
                      private val schemaBasedViewGenerator: SchemaBasedViewGenerator) {

   fun deleteView(viewConfig: CaskConfig):Boolean {
      log().info("Dropping view ${viewConfig.tableName}")
      return try {
         template.execute(CaskViewBuilder.dropViewStatement(viewConfig.tableName))
         log().info("View ${viewConfig.tableName} dropped successfully")
         caskConfigRepository.delete(viewConfig)
         true
      } catch (exception:Exception) {
         log().error("Failed to drop view ${viewConfig.tableName} - ${exception.message}")
         false
      }
   }

   internal fun generateView(viewDefinition: CaskViewDefinition): CaskConfig? {
      log().info("Generating view ${viewDefinition.typeName}")
      val builder = viewBuilderFactory.getBuilder(viewDefinition)
      val viewDdlStatements = builder.generateCreateView()
      return generateView(viewDefinition.typeName, viewDdlStatements) {
         builder.generateCaskConfig()
      }
   }

   internal fun generateView(
      typeName: QualifiedName,
      viewDdlStatements: List<String>,
      caskConfigGenerator: () -> CaskConfig) : CaskConfig? {
      if (viewDdlStatements.isEmpty()) {
         log().warn("No viewDDL generated for $typeName, not proceeding with view creation")
         return null
      }
      try {
         viewDdlStatements.forEach { ddl ->
            log().info("View $typeName executing generated DDL: \n$ddl")
            template.execute(ddl)
         }

         log().info("View $typeName created successfully")

         log().info("Generating cask config for view $typeName")
         val caskConfig = caskConfigGenerator()
         if (!caskConfigRepository.findById(caskConfig.tableName).isPresent) {
            return caskConfigRepository.save(caskConfig)
         }
         log().info("Cask Config for view $typeName created successfully")
         return null
      } catch (e: Exception) {
         log().error("Error in generating view", e)
         return null
      }
   }

   fun generateViews(): List<CaskConfig> {
      log().info("Looking for views to generate")
      val configBasedViews =  viewConfig.views.mapNotNull { viewDefinition ->
         val caskTypeName = viewDefinition.typeName.fullyQualifiedName
         val existingViewCasks = caskConfigRepository.findAllByQualifiedTypeName(caskTypeName)
         if (existingViewCasks.isNotEmpty()) {
            log().info("Not regenerating view $caskTypeName as a cask already exists for this")
            null
         } else {
            log().info("Triggering generation of view $caskTypeName")
            generateView(viewDefinition)
         }
      }

      val schemaBasedViews = generateSchemaBasedViews()
      return configBasedViews + schemaBasedViews
   }

   private fun generateSchemaBasedViews(): List<CaskConfig> {
      val taxiViews = schemaBasedViewGenerator.taxiViews()
      return taxiViews.mapNotNull { view ->
         val caskTypeName = view.qualifiedName
         val existingViewCasks = caskConfigRepository.findAllByQualifiedTypeName(caskTypeName)
         if (existingViewCasks.isNotEmpty()) {
            log().info("Not regenerating view $caskTypeName as a cask already exists for this")
            null
         } else {
            log().info("Triggering generation of view $caskTypeName")
            val viewDdl = schemaBasedViewGenerator.generateDdl(view)
            generateView(view.toQualifiedName(), viewDdl) {
               schemaBasedViewGenerator.generateCaskConfig(view)
            }
         }
      }
   }

   fun getViewDependenciesForType(caskConfig: CaskConfig): List<CaskConfig> {
      val views = viewConfig.views.filter { viewDefinition ->
         viewDefinition.join.types.contains(QualifiedName.from(caskConfig.qualifiedTypeName))
      }.flatMap { viewDefinition ->
         caskConfigRepository.findAllByQualifiedTypeName(viewDefinition.typeName.fullyQualifiedName)
      }
      return views
   }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "cask")
data class CaskViewConfig(
   val views: List<CaskViewDefinition> = emptyList()
)

