package io.vyne.cask.query.generators

import lang.taxi.types.Type
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "cask")
data class OperationGeneratorConfig(val operations: List<OperationConfigDefinition> = emptyList()) {
   companion object {
      fun empty() : OperationGeneratorConfig {
         return OperationGeneratorConfig()
      }
   }
   /**
    * Checks if the given type has any related annotations defined in the configuration.
    * @see <cask/cask_operations.md>
    */
   fun definesOperation(type: Type, annotation: OperationAnnotation): Boolean {
      return this.operations.any { config ->
         (config.applicableTo == type.qualifiedName || type.inheritsFrom.map { t -> t.qualifiedName }.contains(config.applicableTo))
            && config.name == annotation }
   }

   @ConstructorBinding
   data class OperationConfigDefinition(val applicableTo: String, val name: OperationAnnotation)
}

enum class OperationAnnotation(val annotation: String) {
   Association("Association"),
   After("After"),
   Before("Before"),
   Between("Between"),
   Id("Id"),
   // TODO followings should be removed or replace the ghost strings in CaskApiHandler
   FindOne("findOneBy"),
   FindMultipleBy("findMultipleBy"),
   FindSingleBy("findSingleBy"),
   FindAll("findAll"),
   FindBetweemInsertedAt("findBetweenInsertedAt")
}
