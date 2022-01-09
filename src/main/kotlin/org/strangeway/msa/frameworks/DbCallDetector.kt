package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.strangeway.msa.db.getProjectInteractionsService

class DbCallDetector : CallDetector {
  override fun isAvailable(project: Project): Boolean = true

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val interactionsService = getProjectInteractionsService(project)
    if (!interactionsService.hasMethods(getMethodName(uCall))) return null

    val resolved = uCall.resolve() ?: return null

    val mapping = interactionsService.findInteraction(
      resolved.containingClass?.qualifiedName ?: "",
      resolved.name,
      resolved.parameterList.parametersCount
    ) ?: return null

    return MappedInteraction(mapping.interactionType, mapping)
  }

  private fun getMethodName(uCall: UCallExpression): String? {
    if (uCall.kind == UastCallKind.CONSTRUCTOR_CALL) {
      return (uCall.returnType as? PsiClassType)?.name
    }
    return uCall.methodName
  }
}