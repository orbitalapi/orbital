package io.vyne.cockpit.core.workspaces

import java.nio.file.Path
import java.nio.file.Paths

data class WorkspacesConfig(
   val workspacesPath: Path = Paths.get("config/workspaces")
)
