package io.vyne.cask.query

import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ddl.caskRecordTable
import io.vyne.cask.timed
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class CaskConfigService(private val caskConfigRepository: CaskConfigRepository) {
   fun createCaskConfig(versionedType: VersionedType, exposeType: Boolean = false, exposeService: Boolean = true): CaskConfig {
      val config: CaskConfig = timed("CaskDao.addCaskConfig", true, TimeUnit.MILLISECONDS) {
         val tableName = versionedType.caskRecordTable()

         val existing = caskConfigRepository.findByTableName(tableName)
         if (existing != null) {
            log().info("CaskConfig already exists for type=${versionedType.versionedName}, tableName=$tableName")
            return@timed existing
         }
         val config = CaskConfig.forType(
            type = versionedType,
            tableName = tableName,
            exposesType = exposeType,
            exposesService = exposeService,
            daysToRetain = 100000
         )
         log().info("Creating CaskConfig for type=${versionedType.versionedName}, tableName=$tableName")
         caskConfigRepository.save(config)
      }
      return config
   }

}
