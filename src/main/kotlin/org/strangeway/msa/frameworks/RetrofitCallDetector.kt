package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class RetrofitCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "Retrofit Client")

  private val annotations: List<String> = listOf(
    "retrofit2.http.HTTP",
    "retrofit2.http.POST",
    "retrofit2.http.GET",
    "retrofit2.http.HEAD",
    "retrofit2.http.OPTIONS",
    "retrofit2.http.PUT",
    "retrofit2.http.PATCH",
    "retrofit2.http.DELETE"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "retrofit2.http.HTTP")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null

    if (AnnotationUtil.isAnnotated(method, annotations, 0)) {
      return interaction
    }

    return null
  }
}