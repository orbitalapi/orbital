package io.vyne.cask.ingest

import io.vyne.cask.query.CaskConfigService
import io.vyne.cask.query.CaskDAO
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class IngestionEventHandler(
   private val configService:CaskConfigService,
   private val caskDao: CaskDAO) {

   @EventListener
   fun onIngestionInitialised(event: IngestionInitialisedEvent) {
      configService.createCaskConfig(event.type)
      caskDao.createCaskRecordTable(event.type)
   }
}
