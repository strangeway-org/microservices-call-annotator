package org.strangeway.msa.db

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class InteractionsUpdateStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    blockingContext {
      requestDbUpdate(project)
    }
  }
}

fun requestDbUpdate(project: Project, force: Boolean = false) {
  val task = object : Task.Backgroundable(
    project, "Updating microservice interactions database",
    true, ALWAYS_BACKGROUND
  ) {
    override fun run(indicator: ProgressIndicator) {
      getGlobalInteractionsService().updateDatabase(project, indicator, force)
    }
  }

  ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
}