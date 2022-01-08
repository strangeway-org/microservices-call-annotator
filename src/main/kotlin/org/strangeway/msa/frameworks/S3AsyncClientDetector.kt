package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class S3AsyncClientDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.CLOUD_STORAGE, "AWS S3 Client")

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "software.amazon.awssdk.services.s3.S3Client")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    if (clazz.qualifiedName == "software.amazon.awssdk.services.s3.S3AsyncClient") {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null && (
            returnClass.qualifiedName == "java.util.concurrent.CompletableFuture"
                || InheritanceUtil.isInheritor(returnClass, "org.reactivestreams.Publisher"))
      ) {
        return interaction
      }
    }

    if (clazz.qualifiedName == "software.amazon.awssdk.services.s3.S3Client") {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null
        && InheritanceUtil.isInheritor(returnClass, "software.amazon.awssdk.services.s3.model.S3Response")
      ) {
        return interaction
      }
    }

    return null
  }
}