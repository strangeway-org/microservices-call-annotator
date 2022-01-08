package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class MicroprofileRestCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "Microprofile Rest Client")

  private val annotations: List<String> = listOf(
    "javax.ws.rs.GET",
    "javax.ws.rs.HEAD",
    "javax.ws.rs.DELETE",
    "javax.ws.rs.PATCH",
    "javax.ws.rs.OPTIONS",
    "javax.ws.rs.POST",
    "javax.ws.rs.PUT"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "javax.ws.rs.Path")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (clazz.isInterface
      && AnnotationUtil.isAnnotated(clazz, "javax.ws.rs.Path", 0)
      && AnnotationUtil.isAnnotated(method, annotations, 0)
    ) {
      return interaction
    }

    return null
  }
}