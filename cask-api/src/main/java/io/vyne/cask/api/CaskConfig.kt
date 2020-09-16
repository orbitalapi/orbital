package io.vyne.cask.api

import com.vladmihalcea.hibernate.type.array.ListArrayType
import io.vyne.schemas.VersionedType
import org.hibernate.annotations.TypeDef
import java.time.Instant
import javax.persistence.*

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
   @Deprecated("Will be removed")
   @Column(name = "sourceschemaids",  columnDefinition = "text[]")
   @org.hibernate.annotations.Type(type = "list-array")
   val sourceSchemaIds: List<String> = emptyList(),
   @Column(name = "sources",  columnDefinition = "text[]")
   @org.hibernate.annotations.Type(type = "list-array")
   @Deprecated("Will be removed")
   val sources: List<String> = emptyList(),
   @Column(name = "deltaagainsttablename")
   @Deprecated("Will be removed")
   val deltaAgainstTableName: String? = null,
   @Column(name = "insertedat")
   val insertedAt: Instant,
   @Column(name = "exposestype")
   // By default, we don't expose a type, as they already exist elsewhere.
   // For views, we are generating a type, so expose it
   val exposesType: Boolean = false,
   @Column(name = "exposesservice")
   val exposesService: Boolean = true,
   @Column(name = "status")
   @Enumerated(value = EnumType.STRING)
   val status:CaskStatus = CaskStatus.ACTIVE,

   @Column(name = "replacedby")
   val replacedByTableName:String? = null
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

enum class CaskStatus {
   ACTIVE,
   MIGRATING,
   REPLACED
}
data class CaskDetails(
   val recordsNumber: Int
)

