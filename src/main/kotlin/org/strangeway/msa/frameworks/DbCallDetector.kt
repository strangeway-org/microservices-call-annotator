package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.getProjectInteractionsService

class DbCallDetector : CallDetector {
  override fun isAvailable(project: Project): Boolean = true

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val interactionsService = getProjectInteractionsService(project)
    if (!interactionsService.hasMethods(uCall.methodName)) return null

    val resolved = uCall.resolve() ?: return null

    val mapping = interactionsService.findInteraction(
      resolved.containingClass?.qualifiedName ?: "",
      resolved.name,
      resolved.parameterList.parametersCount
    ) ?: return null

    return MappedInteraction(mapping.interactionType, mapping)
  }
}