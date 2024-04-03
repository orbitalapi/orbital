package com.orbitalhq.connectors.hazelcast

import com.orbitalhq.schemas.fqn

object HazelcastTaxi {
   object Annotations {
      internal const val namespace = "com.orbitalhq.hazelcast"
      val HazelcastServiceAnnotation = "${namespace}.HazelcastService"
      val CompactObject = "${namespace}.CompactObject".fqn()
      val JsonObject = "${namespace}.JsonObject".fqn()
      val HazelcastMap = "${namespace}.HazelcastMap".fqn()
      val UpsertOperation = "${namespace}.UpsertOperation".fqn()
      val DeleteOperation = "${namespace}.DeleteOperation".fqn()
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
   annotation DeleteOperation {
      mapName : String
   }

   annotation JsonObject
   annotation CompactObject

}

      """.trimIndent()

}
