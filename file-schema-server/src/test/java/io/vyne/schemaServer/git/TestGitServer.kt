package io.vyne.schemaServer.git

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.http.server.GitServlet
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.io.IOException

/**
 * Starts a simple Git http server.
 * Modified from https://github.com/centic9/jgit-cookbook/blob/master/httpserver/src/main/java/org/dstadler/jgit/server/Main.java
 * Original comments follow...
 * A very simple Git Server which allows to clone a Git repository. Currently
 * it will return the same repository for any name that is requested, there is no
 * logic to distinguish between different repos in this simple example.
 *
 * After starting this application, you can use something like
 *
 * git clone http://localhost:8080/TestRepo
 *
 * to clone the repository from the running server.
 *
 *
 * Note: Visiting http://localhost:8080/&lt;reponame&gt; in the Browser
 * will not work and always return a HTTP Error 404.
 *
 * Also this is just a very simple sample and not a full-features
 * Git Server!
 *
 * Expect some work if you want to do anything useful with this!
 */
class TestGitServer(val port: Int = 20056) {

   companion object {
      fun createStarted(port:Int = 20056):TestGitServer {
         return TestGitServer(port)
      }
   }

   val uri = "http://localhost:$port/TestRepo.git"
   val server: Server

   init {
      val repository = createNewRepository()
      populateRepository(repository)

      // Create the JGit Servlet which handles the Git protocol
      val gs = GitServlet()
      gs.setRepositoryResolver { req, name ->
         repository.incrementOpen()
         repository
      }

      // start up the Servlet and start serving requests
      server = configureAndStartHttpServer(gs)

      // finally wait for the Server being stopped
//      server.join()
   }

   private fun configureAndStartHttpServer(gs: GitServlet): Server {
      val server = Server(port)
      val handler = ServletHandler()
      server.handler = handler
      val holder = ServletHolder(gs)
      handler.addServletWithMapping(holder, "/*")
      server.start()
      return server
   }

   private fun populateRepository(repository: Repository) {
      // enable pushing to the sample repository via http
      repository.config.setString("http", null, "receivepack", "true")
      Git(repository).use { git ->
         val myfile = File(repository.directory.parent, "testfile")
         if (!myfile.createNewFile()) {
            throw IOException("Could not create file $myfile")
         }
         git.add().addFilepattern("testfile").call()
         println("Added file " + myfile + " to repository at " + repository.directory)
         git.commit().setMessage("Test-Checkin").call()
      }
   }

   private fun createNewRepository(): Repository {
      // prepare a new folder
      val localPath = File.createTempFile("TestGitRepository", "")
      if (!localPath.delete()) {
         throw IOException("Could not delete temporary file $localPath")
      }
      if (!localPath.mkdirs()) {
         throw IOException("Could not create directory $localPath")
      }

      // create the directory
      val repository = FileRepositoryBuilder.create(File(localPath, ".git"))
      repository.create()
      return repository
   }

   fun stop() {
      this.server.stop()
   }
}
