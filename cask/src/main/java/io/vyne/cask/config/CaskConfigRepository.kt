package io.vyne.cask.config

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.api.CaskStatus
import org.springframework.data.jpa.repository.JpaRepository

interface CaskConfigRepository : JpaRepository<CaskConfig, String> {
   fun findAllByQualifiedTypeName(typeName: String): List<CaskConfig>
   fun findAllByQualifiedTypeNameAndStatus(typeName: String,status:CaskStatus): List<CaskConfig>
   fun findAllByStatus(status:CaskStatus):List<CaskConfig>
   fun findByTableName(tableName:String):CaskConfig?
   fun findAllByStatusAndExposesType(status:CaskStatus, exposesType: Boolean):List<CaskConfig>
}
