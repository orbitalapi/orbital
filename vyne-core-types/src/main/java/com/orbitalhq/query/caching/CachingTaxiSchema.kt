package com.orbitalhq.query.caching

import com.orbitalhq.VyneTypes
import com.orbitalhq.schemas.fqn

object CacheAnnotation {
   val modeFieldName = "mode"
   val maxIdleSecondsFieldName = "maxIdleSeconds"
   val CacheTypeName = "${VyneTypes.NAMESPACE}.caching.Cache".fqn()
   val CacheTaxi = """
namespace ${CacheTypeName.namespace} {

   enum CachePolicy {
      Enabled,
      Disabled
   }
   annotation Cache {
      $modeFieldName: CachePolicy = CachePolicy.Enabled
      connection : String?
      [[ If an entry is not accessed (read or write) for this duration, it is evicted from the cache ]]
      $maxIdleSecondsFieldName: Int = 180
   }
}
   """.trimIndent()
}
