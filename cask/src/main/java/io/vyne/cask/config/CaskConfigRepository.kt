package io.vyne.cask.config

import io.vyne.cask.api.CaskConfig
import org.springframework.data.jpa.repository.JpaRepository

interface CaskConfigRepository : JpaRepository<CaskConfig, String> {
   fun findAllByQualifiedTypeName(typeName: String): List<CaskConfig>
}
