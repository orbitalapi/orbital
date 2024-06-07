package com.orbitalhq.copilot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vyne.copilot")
data class CopilotSettings(
   val model: Model = Model.gpt_4o,
   val endpointUrl: String = "https://api.openai.com/v1/chat/completions",
   val apiKey: String = "",
)

enum class Model(val id: String) {
   gpt_4("gpt-4"),
   gpt_4o("gpt-4o"),
   gpt_4o_2024_05_13("gpt-4o-2025-05-13"),
   gpt_4_turbo("gpt-4_turbo"),
   gpt_35_turbo("gpt-3.5-turbo")
}
