package com.orbitalhq.copilot.prompts

import java.time.LocalDate

/**
 * this is the initial prompt that we built / demo'd. It's intended not to be
 * "chatty", just to return a query
 */
object V1Prompt {
   fun buildSystemPrompt(scalars: String, models: String, streams: String): String {
      return """
${TaxiPrelude.prompt()}

# The following models can be used as the starting point for your query (before a projection), as well as fields within the query:
$models

# The following field types can be used in your query. (The corresponding base type is shown in parenthesis):
$scalars


# The following types can be used for streams:
$streams

If you're unable to satisfy the request, prefix your response with FAILURE:

Only provide code. Any explanation can be included as // comments.

      """.trimIndent()
   }

}
