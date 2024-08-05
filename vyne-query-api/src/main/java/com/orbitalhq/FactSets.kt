package com.orbitalhq

typealias FactSetId = String


object FactSets {
   const val DEFAULT: FactSetId = "DEFAULT"

   /**
    * Facts about the current user / principal
    */
   const val AUTHENTICATION: FactSetId = "AUTHENTICATION"
   const val ALL: FactSetId = "@@ALL"
   const val NONE: FactSetId = "@@NONE"

}
