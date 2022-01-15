package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class GoogleFirestoreCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "Google Cloud Firestore")

  private val apiPackage: String = "com.google.cloud.firestore."

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "com.google.api.core.ApiFuture")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    val qualifiedName = clazz.qualifiedName ?: ""
    if (qualifiedName.startsWith(apiPackage)) {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null && returnClass.qualifiedName == "com.google.api.core.ApiFuture") {
        return interaction
      }
    }

    return null
  }
}