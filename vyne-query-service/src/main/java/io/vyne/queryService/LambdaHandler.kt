package io.vyne.queryService

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class ServiceHandler : RequestHandler<String?, Any?> {
   override fun handleRequest(s: String?, context: Context?): Any {
      context!!.logger.log("Input: $s")
      return "Lambda Function is invoked....$s"
   }
}
