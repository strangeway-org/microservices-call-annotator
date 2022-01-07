package org.strangeway.msa.db

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class InteractionsUpdateStartupActivity : StartupActivity.Background {
  override fun runActivity(project: Project) {
    getGlobalInteractionsService().updateDatabase()
  }
}