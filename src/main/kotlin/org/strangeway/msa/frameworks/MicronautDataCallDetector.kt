package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression

class MicronautDataCallDetector : CallDetector {
  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    return null
  }

  override fun isAvailable(project: Project): Boolean {
    return false
  }
}