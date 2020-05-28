package io.vyne.cask.api

data class CaskIngestionResponse(val result : ResponseResult, val message : String?) {
   enum class ResponseResult {
      SUCCESS,
      WARNING,
      REJECTED
   }

   companion object {
      @JvmStatic
      fun success(message: String?): CaskIngestionResponse {
         return CaskIngestionResponse(ResponseResult.SUCCESS, message)
      }
      @JvmStatic
      fun rejected(message: String): CaskIngestionResponse {
         return CaskIngestionResponse(ResponseResult.REJECTED, message)
      }
   }
}
