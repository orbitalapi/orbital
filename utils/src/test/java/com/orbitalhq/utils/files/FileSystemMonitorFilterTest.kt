package com.orbitalhq.utils.files

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class FileSystemMonitorFilterTest {

    @Test
    fun `should monitor files with valid extensions`() {
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/schema.taxi")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/config.conf")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/build.kts")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/schema.avsc")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/service.proto")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/api.yaml")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/data.json")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/service.wsdl")))
        assertTrue(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/config.xml")))
    }

    @Test
    fun `should not monitor files with invalid extensions`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/script.sh")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/app.py")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/code.kt")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/readme.md")))
    }

    @Test
    fun `should not monitor git directory`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/.git/config")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/.git/HEAD")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/.git/objects/abc123")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/subdir/.git/config")))
    }

    @Test
    fun `should not monitor vim swap files`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/schema.taxi.swp")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/.schema.taxi.swp")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/subdir/config.conf.swp")))
    }

    @Test
    fun `should not monitor macOS temporary files`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/._schema.taxi")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/subdir/._config.conf")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/._DS_Store")))
    }

    @Test
    fun `should not monitor temporary files`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/temp.tmp")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/cache.tmp")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/subdir/data.tmp")))
    }

    @Test
    fun `should not monitor backup files`() {
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/schema.taxi~")))
        assertFalse(FileSystemMonitorFilter.shouldMonitor(Paths.get("/project/config.conf~")))
    }

    @Test
    fun `should respect excluded directory names`() {
        assertFalse(
            FileSystemMonitorFilter.shouldMonitor(
                Paths.get("/project/node_modules"),
                listOf("node_modules")
            )
        )
        assertFalse(
            FileSystemMonitorFilter.shouldMonitor(
                Paths.get("/project/build"),
                listOf("build", "dist")
            )
        )
    }

    @Test
    fun `shouldMonitorFileName works correctly`() {
        // Valid files
        assertTrue(FileSystemMonitorFilter.shouldMonitorFileName("schema.taxi", "taxi"))
        assertTrue(FileSystemMonitorFilter.shouldMonitorFileName("config.yaml", "yaml"))

        // Invalid patterns
        assertFalse(FileSystemMonitorFilter.shouldMonitorFileName("._schema.taxi", "taxi"))
        assertFalse(FileSystemMonitorFilter.shouldMonitorFileName("schema.taxi.swp", "swp"))
        assertFalse(FileSystemMonitorFilter.shouldMonitorFileName("temp.tmp", "tmp"))
        assertFalse(FileSystemMonitorFilter.shouldMonitorFileName("schema.taxi~", "taxi~"))
        assertFalse(FileSystemMonitorFilter.shouldMonitorFileName("code.kt", "kt"))
    }
}
