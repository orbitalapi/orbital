package com.orbitalhq.plugins

interface Plugin {
   val name: String
   fun initialize()
}
