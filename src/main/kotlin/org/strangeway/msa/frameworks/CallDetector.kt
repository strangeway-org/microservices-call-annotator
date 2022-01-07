package org.strangeway.msa.frameworks

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionMapping
import org.strangeway.msa.db.InteractionType

interface CallDetector {
  fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction?

  fun isAvailable(project: Project): Boolean

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<CallDetector> =
      ExtensionPointName.create("org.strangeway.msa.callDetector")

    @JvmStatic
    fun getCallDetectors(project: Project): List<CallDetector> {
      return CachedValuesManager.getManager(project).getCachedValue(project) {
        val detectors = EP_NAME.extensionList.filter { it.isAvailable(project) }

        Result.create(detectors, ProjectRootManager.getInstance(project))
      }
    }
  }
}

data class Interaction(
  val type: InteractionType,
  val mapping: InteractionMapping?
)