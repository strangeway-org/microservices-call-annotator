package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class MicronautHttpCallDetector : CallDetector {

  private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "Micronaut HTTP Client")

  private val annotations: List<String> = listOf(
    "io.micronaut.http.annotation.Get",
    "io.micronaut.http.annotation.Post",
    "io.micronaut.http.annotation.Put",
    "io.micronaut.http.annotation.Patch",
    "io.micronaut.http.annotation.Delete",
    "io.micronaut.http.annotation.Head",
    "io.micronaut.http.annotation.Options",
    "io.micronaut.http.annotation.CustomHttpMethod"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "io.micronaut.http.client.annotation.Client")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (AnnotationUtil.isAnnotated(clazz, "io.micronaut.http.client.annotation.Client", 0)
      && AnnotationUtil.isAnnotated(method, annotations, 0)
    ) {
      return interaction
    }

    return null
  }
}