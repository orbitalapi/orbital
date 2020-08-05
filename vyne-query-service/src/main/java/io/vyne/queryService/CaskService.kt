package io.vyne.queryService

import io.vyne.cask.api.CaskApi
import io.vyne.cask.api.EvictionParameters
import io.vyne.cask.api.EvictionScheduleParameters
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RestController

/**
 * This class provides simple proxying to Cask
 */
@RestController
class CaskService(private val caskApi: CaskApi): CaskApi by caskApi
