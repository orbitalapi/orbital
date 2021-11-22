package io.vyne.schemas

import io.vyne.schemaStore.SchemaSet
import lang.taxi.utils.log

data class SchemaSetChangedEvent(val oldSchemaSet: SchemaSet?, val newSchemaSet: SchemaSet) {
   companion object {
      fun generateFor(oldSchemaSet: SchemaSet?, newSchemaSet: SchemaSet):SchemaSetChangedEvent? {
         val oldId = oldSchemaSet?.id
         val newId = newSchemaSet.id
         val oldGeneration = oldSchemaSet?.generation
         val newGeneration = newSchemaSet.generation

         when {
            oldId == newId && oldGeneration == newGeneration -> {
               log().info("Not generating a SchemaSetChangedEvent, as both id and generation are the same (id = $oldId, generation = $oldGeneration)")
               return null
            }
            oldId == newId && oldGeneration != newGeneration -> {
               log().info("Not generating a SchemaSetChangedEvent, as both ids are the same (id = $oldId).  However, the generation has changed from $oldGeneration to $newGeneration, which suggests nodes may be out of sync")
               return null
            }
            oldId != newId && oldGeneration == newGeneration -> {
               log().warn("Detected an anomaly while generating a schema change event -- the content id has changed from $oldId to $newId, but the generation remains unchanged at $oldGeneration.  This needs investigation")
               return SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)
            }
            else -> {
               log().info("SchemaSet has changed from id=$oldId, generation = $oldGeneration to id=$newId, generation = $newGeneration")
               return SchemaSetChangedEvent(oldSchemaSet, newSchemaSet)
            }
         }
      }
   }
}
