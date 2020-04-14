package io.vyne.schemas

data class SourceCode(
   val origin: String,
   val language: String,
   val content: String
) {
   companion object {
      fun undefined(language: String): SourceCode {
         return SourceCode("Unknown", language, "")
      }

      fun native(language: String): SourceCode {
         return SourceCode("Native", language, "")
      }
   }
}
