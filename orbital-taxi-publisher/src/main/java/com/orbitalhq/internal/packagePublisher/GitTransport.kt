package com.orbitalhq.internal.packagePublisher

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialItem.CharArrayType
import org.eclipse.jgit.transport.CredentialItem.InformationalMessage
import org.eclipse.jgit.transport.CredentialItem.StringType
import org.eclipse.jgit.transport.CredentialItem.YesNoType
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import picocli.CommandLine
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess


class GitTransport(
    private val config: OrbitalPackagePublisherConfig,
    private val commandLine: CommandLine
) {

    private fun printOut(message: String) = commandLine.out.println(message)
    private var git: Git

    init {
        val basePath = Paths.get(config.outputPath)
        val basePathFile = basePath.toFile()
        basePathFile.parentFile.mkdirs()
        if (basePathFile.exists() && !basePathFile.isDirectory) {
            printOut("${basePath.toFile()} is not directory!")
            exitProcess(1)
        }
        if (!basePathFile.exists()) {
            basePathFile.mkdir()
        }


        if (basePathFile.listFiles().isNotEmpty()) {
            printOut("$basePathFile is not empty!")
            exitProcess(1)
        }

        printOut("Cloning to ${basePathFile.absolutePath}")
        if (config.remoteRepositoryUrl != null) {
            printOut("Cloning from remote repository ${config.remoteRepositoryUrl}")

        }
        git = cloneRepository(basePathFile)
        checkout(git)
    }


    private fun cloneRepository(localPath: File): Git {
        printOut("Cloning repository...")
        printOut("Using the ssh key file => ${config.sshKeyPath}")
        val remoteUrl = config.remoteRepositoryUrl ?: "file://${localPath.absolutePath}"
        return Git.cloneRepository()
            .setURI(remoteUrl)
            .setDirectory(localPath)
            .setCloneAllBranches(true)
            .setTransportConfigCallback { transport ->
                if (transport is SshTransport) {
                    transport.credentialsProvider = CustomCredentialProvider()
                    transport.sshSessionFactory = CustomSshFactory(config.sshKeyPath)
                }
            }
            .call()
    }

    private fun checkout(git: Git) {
        val current = currentBranch(git)
        if (current == config.version) {
            printOut("Already on branch ${config.version}")
        }

        val localBranchExists = localBranchExists(git, config.version)
        //refs/remotes/origin/main
        val remoteExists = remoteBranchExists(git, "origin", config.version)

        when {
            localBranchExists -> git.checkout().setName(config.version).call()
            remoteExists -> git.checkout().setName(config.version).setCreateBranch(true).setForce(true)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint("origin" + "/" + config.version).call()
            else -> {
                git.branchCreate().setName(config.version).setForce(true).call();
                git.checkout().setName(config.version).call();
                pushWithSsh()
                    .setRemote("origin")
                    .setRefSpecs(RefSpec(config.version))
                    .setForce(true)
                    .call()
            }
        }
    }

    private fun currentBranch(git: Git): String? {
        return try {
            git.repository.branch
        } catch (e: IOException) {
            null
        }
    }

    private fun localBranchExists(git: Git, branch: String): Boolean {
        val list = git.branchList().call()
        val fullName = "refs/heads/$branch"
        var exists = false
        for (ref in list) {
            val name = ref.name
            if (name == fullName) {
                exists = true
            }
        }
        return exists
    }

    private fun remoteBranchExists(git: Git, remote: String, branch: String): Boolean {
        val list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
        val fullName = "refs/remotes/$remote/$branch"
        var exists = false
        for (ref in list) {
            val name = ref.name
            if (name == fullName) {
                exists = true
            }
        }
        return exists
    }

    fun commitAndTryPush() {
        printOut("Committing changes...")
        git.add().addFilepattern(".").call()
        git.commit()
            .setMessage("Automated update")
            .setAllowEmpty(true)
            .call()

        config.tagName?.let {
            printOut("Adding tag: $it")
            git
                .tag()
                .setForceUpdate(true)
                .setName(it).call()
        }

        if (config.remoteRepositoryUrl != null) {
            printOut("Pushing changes...")
            git
            pushWithSsh().call()
            if (config.tagName != null) {
                pushWithSsh()
                    .setForce(true)
                    .setPushTags().call()
            }
        } else {
            printOut("--remote-url is not specified not pushing the changes.")
        }

    }

    private fun pushWithSsh(): PushCommand {
        return git.push()
            .setTransportConfigCallback { transport ->
                if (transport is SshTransport) {
                    transport.credentialsProvider = CustomCredentialProvider()
                    transport.sshSessionFactory = CustomSshFactory(config.sshKeyPath)
                }
            }

    }
}

class CustomSshFactory(private val sshKeyFilePath: String) : SshdSessionFactory() {
    private val tempDirectoryForSshSessionFactory = Files.createTempDirectory("ssh-temp-dir")

    override fun getSshDirectory(): File {
        return tempDirectoryForSshSessionFactory.toFile()
    }

    override fun getDefaultIdentities(sshDir: File): MutableList<Path> {
        val keyFilePath = tempDirectoryForSshSessionFactory.resolve("keyfile")
        Files.write(keyFilePath, File(sshKeyFilePath).readBytes())
        return mutableListOf(keyFilePath)
    }
}

class CustomCredentialProvider(private val passPhrase: String? = null) : CredentialsProvider() {
    override fun isInteractive(): Boolean {
        return false
    }

    override fun supports(vararg p0: CredentialItem?): Boolean {
        return true
    }

    override fun get(p0: URIish?, vararg items: CredentialItem): Boolean {
        for (item in items) {
            if (item is InformationalMessage) {
                continue
            }

            if (item is YesNoType) {
                item.value = true
            } else if (item is CharArrayType) {
                if (passPhrase != null) {
                    item.value = passPhrase.toCharArray()
                } else {
                    return false
                }
            } else if (item is StringType) {
                if (passPhrase != null) {
                    item.value = passPhrase
                } else {
                    return false
                }
            } else {
                return false
            }
        }
        return true
    }

}