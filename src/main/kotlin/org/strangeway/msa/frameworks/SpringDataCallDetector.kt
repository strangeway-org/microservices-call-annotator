package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class SpringDataCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "Spring Data Repository")

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "org.springframework.stereotype.Repository")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (AnnotationUtil.isAnnotated(method, "org.springframework.data.r2dbc.repository.Query", 0)
      || AnnotationUtil.isAnnotated(clazz, "org.springframework.stereotype.Repository", 0)
      || InheritanceUtil.isInheritor(clazz, "org.springframework.data.repository.Repository")
    ) {
      return interaction
    }

    return null
  }
}