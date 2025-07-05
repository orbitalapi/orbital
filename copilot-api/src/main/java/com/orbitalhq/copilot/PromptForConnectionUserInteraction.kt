package com.orbitalhq.copilot

import com.orbitalhq.connectors.registry.ConnectorCategory
import com.orbitalhq.connectors.registry.VendorConnectionType

/**
 * Prompts the user to select a data source, or create a new one.
 */
data class PromptForConnectionUserInteraction(
   override val prompt: String,
   val vendorConnectionType: VendorConnectionType
) : UserInteractionPrompt {
   override val intent: UserInteractionPrompt.Intent = UserInteractionPrompt.Intent.SelectDataSource

   val connectionCategory: ConnectorCategory = vendorConnectionType.connectorType
}


/**
 * An interface for sending structured UI wizard interactions
 * in the conversation
 */
interface UserInteractionPrompt {
   val intent: Intent
   val prompt: String

   enum class Intent {
      SelectDataSource
   }
}
