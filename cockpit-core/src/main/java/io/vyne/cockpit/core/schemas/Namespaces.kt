package io.vyne.cockpit.core.schemas

object Namespaces {
   fun hostToNamespace(host: String?): String? {
      if (host == null) return null
      return host.split(".").reversed().joinToString(".")
   }
}
