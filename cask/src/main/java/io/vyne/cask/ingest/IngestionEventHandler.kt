package io.vyne.cask.ingest

import io.vyne.cask.query.CaskConfigService
import io.vyne.cask.query.CaskDAO
import org.springframework.stereotype.Component

@Component
class IngestionEventHandler(
   private val configService:CaskConfigService,
   private val caskDao: CaskDAO) {

   fun onIngestionInitialised(event: IngestionInitialisedEvent) {
      configService.createCaskConfig(event.type)
      caskDao.createCaskRecordTable(event.type)
   }
}
