package com.orbitalhq.connectors.hazelcast

import com.orbitalhq.schemas.fqn

object HazelcastTaxi {
   object Annotations {
      internal const val namespace = "com.orbitalhq.hazelcast"
      val HazelcastServiceAnnotation = "${namespace}.HazelcastService"
      val CompactObject = "${namespace}.CompactObject".fqn()
      val JsonObject = "${namespace}.JsonObject".fqn()
      val HazelcastMap = "${namespace}.HazelcastMap".fqn()
   }
   val schema = """
namespace ${Annotations.namespace} {
   annotation HazelcastService {
      connectionName : String
   }
   annotation HazelcastMap {
      name : HazelcastMapName inherits String
   }
   annotation UpsertOperation {}
   annotation InsertOperation {}
   annotation UpdateOperation {}

   annotation JsonObject
   annotation CompactObject

}

      """.trimIndent()

}
