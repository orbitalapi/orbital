package io.vyne.schemaServer.core

import com.winterbe.expekt.should
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
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
      val empty = FileSchemaRepositoryConfigLoader(Paths.get("/this/path/doesnt/exist"))
         .load()
      empty.file.should.be.`null`
      empty.git.should.be.`null`
      empty.openApi.should.be.`null`
   }

   @Test
   fun `can read and write a full config`() {
      val config = SchemaRepositoryConfig(
         file = FileSystemSchemaRepositoryConfig(
            paths = listOf(
               Paths.get("/a/b/c/project")
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
         openApi = OpenApiSchemaRepositoryConfig(
            services = listOf(
               OpenApiSchemaRepositoryConfig.OpenApiServiceConfig(
                  "some-service",
                  "https://foo.com/swagger.json",
                  defaultNamespace = "com.foo.bar"
               )
            )
         )
      )

      val path = folder.root.toPath().resolve("repo.conf")
      val configRepo = FileSchemaRepositoryConfigLoader(path)
      configRepo.save(config)
      val loaded = configRepo.load()
      loaded.should.equal(config)
   }


}
