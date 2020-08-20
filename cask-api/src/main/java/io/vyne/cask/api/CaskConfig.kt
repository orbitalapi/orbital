package io.vyne.cask.api

import com.vladmihalcea.hibernate.type.array.ListArrayType
import io.vyne.schemas.VersionedType
import org.hibernate.annotations.TypeDef
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "cask_config")
@TypeDef(name = "list-array", typeClass = ListArrayType::class)
data class CaskConfig(
   @Id
   @Column(name = "tablename")
   val tableName: String,
   @Column(name = "qualifiedtypename")
   val qualifiedTypeName: String,
   @Column(name = "versionhash")
   val versionHash: String,
   @Column(name = "sourceschemaids",  columnDefinition = "text[]")
   @org.hibernate.annotations.Type(type = "list-array")
   val sourceSchemaIds: List<String>,
   @Column(name = "sources",  columnDefinition = "text[]")
   @org.hibernate.annotations.Type(type = "list-array")
   val sources: List<String>,
   @Column(name = "deltaagainsttablename")
   val deltaAgainstTableName: String?,
   @Column(name = "insertedat")
   val insertedAt: Instant,
   @Column(name = "exposestype")
   // By default, we don't expose a type, as they already exist elsewhere.
   // For views, we are generating a type, so expose it
   val exposesType: Boolean = false,
   @Column(name = "exposesservice")
   val exposesService: Boolean = true,
   @Column(name = "daysToRetain")
   val daysToRetain: Int
) {
   companion object {
      fun forType(
         type:VersionedType,
         tableName: String,
         insertionTime:Instant = Instant.now(),
         deltaAgainstTableName: String? = null,
         exposesType: Boolean = false,
         exposesService: Boolean = true
      ):CaskConfig {
         return CaskConfig(
            tableName,
            type.fullyQualifiedName,
            type.versionHash,
            type.sourceSchemaIds,
            type.sourceContent,
            deltaAgainstTableName,
            insertionTime,
            exposesType,
            exposesService
         )
      }
   }
}

data class CaskDetails(
   val recordsNumber: Int
)

