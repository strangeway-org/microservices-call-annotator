package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class SpringFeignCallDetector : CallDetector {

  private val interaction: Interaction = FrameworkInteraction(InteractionType.REQUEST, "Spring Feign Client")

  private val annotations: List<String> = listOf(
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.RequestMapping"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "org.springframework.cloud.openfeign.FeignClient")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (AnnotationUtil.isAnnotated(clazz, "org.springframework.cloud.openfeign.FeignClient", 0)
      && AnnotationUtil.isAnnotated(method, annotations, 0)
    ) {
      return interaction
    }

    return null
  }
}