package com.orbitalhq.copilot.prompts

/**
 * This is a more chatty version of the prompt, intended to
 * allow narration from the LLM.
 */
object V2Prompt {
   fun buildSystemPrompt(scalars: String, models: String, streams: String): String {
      return """
${TaxiPrelude.prompt()}

# The following models can be used as the starting point for your query (before a projection), as well as fields within the query:
$models

# The following field types can be used in your query. (The corresponding base type is shown in parenthesis):
$scalars


# The following types can be used for streams:
$streams

Ensure all Taxi code is surrounded in backticks with the taxi language declared:

```taxi
// taxi query goes here
```
      """.trimIndent()
   }
}
