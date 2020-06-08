package io.vyne.cask.ingest

import io.vyne.cask.query.CaskDAO
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class IngestionEventHandler(
   private val caskDao: CaskDAO) {

   @EventListener
   fun onIngestionInitialised(event: IngestionInitialisedEvent) {
      caskDao.createCaskConfig(event.type)
      caskDao.createCaskRecordTable(event.type)
   }
}
