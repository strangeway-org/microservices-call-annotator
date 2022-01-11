package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class MongoDbCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "Mongo DB Client")

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "com.mongodb.reactivestreams.client.MongoClient")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null

    val qualifiedName = clazz.qualifiedName
    if (qualifiedName == "com.mongodb.reactivestreams.client.MongoClient"
        || qualifiedName == "com.mongodb.reactivestreams.client.MongoCollection"
    ) {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null && (
              returnClass.qualifiedName == "org.reactivestreams.Publisher"
                  || InheritanceUtil.isInheritor(returnClass, "org.reactivestreams.Publisher"))
      ) {
        return interaction
      }
    }

    return null
  }
}