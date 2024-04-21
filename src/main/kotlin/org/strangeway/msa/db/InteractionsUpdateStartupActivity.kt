package org.strangeway.msa.db

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class InteractionsUpdateStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    getGlobalInteractionsService().scheduleUpdate(project)
  }
}

