package io.vyne.cask.observers

import lang.taxi.types.Type

interface IngestionObserver {
   fun isObservable(taxiType: Type): Boolean
   fun kafkaObserverConfig(taxiType: Type): KafkaObserverConfiguration
}
