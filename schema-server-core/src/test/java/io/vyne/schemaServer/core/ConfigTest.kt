package io.vyne.schemaServer.core

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.schemaServer.core.adaptors.OpenApiPackageLoaderSpec
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

class ConfigTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `returns an empty config if config file doesn't exist`() {
      val empty = FileSchemaRepositoryConfigLoader(Paths.get("/this/path/doesnt/exist"),
         eventDispatcher = mock { })
         .load()
      empty.file.should.be.`null`
      empty.git.should.be.`null`
   }

   @Test
   fun `can read and write a full config`() {
      val config = SchemaRepositoryConfig(
         file = FileSystemSchemaRepositoryConfig(
            projects = listOf(
               FileSystemPackageSpec(
                  path = Paths.get("/a/b/c/project"),
                  editable = true
               )

            )
         ),
         git = GitSchemaRepositoryConfig(
            checkoutRoot = Paths.get("/my/git/root"),
            repositories = listOf(
               GitRepositoryConfig(
                  "my-git-project",
                  "https://github.com/something.git",
                  branch = "master"
               )
            )
         ),

         )

      val path = folder.root.toPath().resolve("repo.conf")
      val configRepo = FileSchemaRepositoryConfigLoader(path,
         eventDispatcher = mock { })
      configRepo.save(config)
      val loaded = configRepo.load()
      loaded.should.equal(config)
   }


   @Test
   fun `file paths in config file are treated as relative if not explicitly absolute`() {
      val configFile = Resources.getResource("config-files/with-relative-path.conf")
         .toURI()
      val targetConfigFile = folder.newFile("server.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileSchemaRepositoryConfigLoader(targetConfigFile.toPath(),
         eventDispatcher = mock { })
      val config = configRepo.load()
      config.file!!.projects.should.have.size(1)
      val path = config.file!!.projects[0].path

      path.should.equal(folder.root.resolve("path/to/project").toPath())
   }

   @Test
   fun `can read a file with schema projects configured`() {
      val configFile = Resources.getResource("config-files/with-package-project.conf")
         .toURI()
      val targetConfigFile = folder.newFile("server.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileSchemaRepositoryConfigLoader(targetConfigFile.toPath(),
         eventDispatcher = mock { })
      val config = configRepo.load()

      config.file!!.projects.should.have.size(2)
      val loader = config.file!!.projects[1].loader as OpenApiPackageLoaderSpec
      loader.uri.toString().should.equal("http://acme.com/api/open-api")

      configRepo.save(config)
   }

}
