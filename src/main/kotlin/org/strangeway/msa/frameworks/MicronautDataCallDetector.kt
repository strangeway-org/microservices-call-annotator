package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class MicronautDataCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "Micronaut Data Repository")

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "io.micronaut.data.repository.GenericRepository")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (AnnotationUtil.isAnnotated(method, "io.micronaut.data.annotation.Query", 0)
      || AnnotationUtil.isAnnotated(clazz, "io.micronaut.data.annotation.Repository", 0)
      || InheritanceUtil.isInheritor(clazz, "io.micronaut.data.repository.GenericRepository")
    ) {
      return interaction
    }

    return null
  }
}