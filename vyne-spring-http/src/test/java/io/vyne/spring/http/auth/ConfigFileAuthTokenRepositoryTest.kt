package io.vyne.spring.http.auth

import com.google.common.io.Resources
import com.typesafe.config.ConfigFactory
import com.winterbe.expekt.should
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI

class ConfigFileAuthTokenRepositoryTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `does not throw exceptions if config file does not exist`() {
      val repository = ConfigFileAuthTokenRepository(folder.root.toPath().resolve("not-there.conf"))
      repository.getToken("Foo").should.be.`null`
   }

   @Test
   fun `can delete token`() {
      val configFile = configFileInTempFolder("auth/sample.conf")
      val repository = ConfigFileAuthTokenRepository(configFile.toPath())
      repository.saveToken(
         "NewService",
         AuthToken(
            tokenType = AuthTokenType.Header,
            value = "abc123",
            paramName = "Authorization",
            valuePrefix = "Bearer"
         )
      )
      repository.deleteToken("NewService")

      val writtenSource = configFile.readText()
      repository.getToken("NewService").should.be.`null`
      // Existing services should remain unaffected after a delete action
      repository.getToken("MyService").should.not.be.`null`
   }

   @Test
   fun `can add and remove services with namespaces`() {
      val configFile = configFileInTempFolder("auth/sample.conf")
      val repository = ConfigFileAuthTokenRepository(configFile.toPath())
      repository.saveToken(
         "com.foo.bar.MyService", AuthToken(
         tokenType = AuthTokenType.Header,
         value = "abc123",
         paramName = "Authorization",
         valuePrefix = "Bearer"
      )
      )

      repository.getToken("com.foo.bar.MyService")!!.value.should.equal("abc123")
      repository.deleteToken("com.foo.bar.MyService")

      val writtenSource = configFile.readText()
      writtenSource.should.not.be.empty
   }

   @Test
   fun `can write a new auth token to file`() {
      var repository = ConfigFileAuthTokenRepository(folder.root.toPath().resolve("auth.conf"))
      val token = AuthToken(
         tokenType = AuthTokenType.Header,
         value = "Hello, World",
         paramName = "Authorization",
         valuePrefix = "Bearer"
      )

      repository.saveToken(
         "MyService", token
      )

      val writtenSource = folder.root.resolve("auth.conf").readText()
      writtenSource.should.not.be.empty

      // Now recreate and reload to ensure persistence works
      repository = ConfigFileAuthTokenRepository(folder.root.toPath().resolve("auth.conf"))
      repository.getToken("MyService").should.equal(token)
   }

   @Test
   fun `can read value from disk`() {
      val configFile = configFileInTempFolder("auth/sample.conf")
      val repository = ConfigFileAuthTokenRepository(configFile.toPath())
      val token = repository.getToken("MyService")
      token!!.tokenType.should.equal(AuthTokenType.Header)
      token.value.should.equal("Hello, World")
      token.valuePrefix.should.equal("Bearer")
      token.paramName.should.equal("Authorization")
   }

   @Test
   fun `can read value with substitution`() {
      val configFile = configFileInTempFolder("auth/sample-with-substitution.conf")
      val fallback = ConfigFactory.parseMap(
         mapOf("foo" to "bar")
      )
      val repository = ConfigFileAuthTokenRepository(configFile.toPath(), fallback)
      val token = repository.getToken("MyService")
      token!!.tokenType.should.equal(AuthTokenType.Header)
      token.value.should.equal("bar")
      token.valuePrefix.should.equal("Bearer")
      token.paramName.should.equal("Authorization")
   }

   @Test
   fun `can write substitutable value which is replaced on read`() {
      val repository = ConfigFileAuthTokenRepository(
         configFileInTempFolder("auth/sample-with-substitution.conf").toPath(),
         fallback = ConfigFactory.parseMap(
            mapOf(
               "foo" to "bar",
               "baz" to "blammo"
            )
         )
      )
      repository.saveToken(
         "NewService", AuthToken(
         tokenType = AuthTokenType.Header,
         paramName = "Authorization",
         valuePrefix = "Bearer",
         value = "\${baz}"
      )
      )

      val writtenSource = folder.root.resolve("sample-with-substitution.conf").readText()
      writtenSource.should.contain("\${foo}")
      writtenSource.should.contain("\${baz}")

      // Make sure that substituted values were not written on the way out
      writtenSource.should.not.contain("bar")
      writtenSource.should.not.contain("blammo")

      repository.getToken("NewService")!!.value.should.equal("blammo")
      repository.getToken("MyService")!!.value.should.equal("bar")
   }

   private fun configFileInTempFolder(resourceName: String): File {
      return Resources.getResource(resourceName).toURI()
         .copyTo(folder.root)
   }
}

fun String.copyResourceTo(destDirectory: File): File {
   return Resources.getResource(this).toURI()
      .copyTo(destDirectory)
}

fun URI.copyTo(destDirectory: File): File {
   val source = File(this)
   val destFile = destDirectory.resolve(source.name)
   FileUtils.copyFile(source, destFile)
   return destFile
}
