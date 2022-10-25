package io.vyne.schemaServer.core.git

import io.vyne.schemaServer.core.adaptors.PackageLoaderSpec
import io.vyne.schemaServer.core.adaptors.taxi.TaxiPackageLoaderSpec
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

data class GitSchemaRepositoryConfig(
   val checkoutRoot: Path? = null,
   val pollFrequency: Duration = Duration.ofSeconds(30),
   val repositories: List<GitRepositoryConfig> = emptyList(),
) {

}

data class GitRepositoryConfig(
   val name: String,
   val uri: String,
   val branch: String,
   val sshPrivateKeyPath: String? = null,
   val sshPassPhrase: String? = null,
   /**
    * The Path within the repository
    */
   val path: Path = Paths.get("/"),
   val loader: PackageLoaderSpec = TaxiPackageLoaderSpec
) {
   val description: String = "$name - $uri / $branch"
}
