package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class RetrofitCallDetector : CallDetector {
  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    return null
  }

  override fun isAvailable(project: Project): Boolean {
    return false
  }
}