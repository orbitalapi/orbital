package io.vyne.cask.api

data class CaskIngestionResponse(val result: ResponseResult, val inputMessage: String?, val message: String?) {
   enum class ResponseResult {
      SUCCESS,
      WARNING,
      REJECTED
   }

   companion object {
      @JvmStatic
      fun success(inputMessage: String, infoMessage: String?): CaskIngestionResponse {
         return CaskIngestionResponse(ResponseResult.SUCCESS, inputMessage, infoMessage)
      }

      @JvmStatic
      fun rejected(inputMessage: String, errorMessage: String): CaskIngestionResponse {
         return CaskIngestionResponse(ResponseResult.REJECTED, inputMessage, errorMessage)
      }
   }
}
