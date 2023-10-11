package com.orbitalhq.spring.utils

import org.springframework.boot.info.BuildProperties

fun BuildProperties?.versionOrDev(): String {
   val baseVersion = this?.get("baseVersion")
   val buildNumber = this?.get("buildNumber")
   val isDevBuild = this?.version?.orEmpty()?.contains("SNAPSHOT") ?: true
   return if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && isDevBuild) {
      "$baseVersion-BETA-$buildNumber"
   } else {
      this?.version ?: "Dev version"
   }

}
