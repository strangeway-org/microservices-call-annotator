package org.strangeway.msa.frameworks

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class JdbiCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "JDBI Repository")

  private val annotations: List<String> = listOf(
    "org.jdbi.v3.sqlobject.statement.SqlUpdate",
    "org.jdbi.v3.sqlobject.statement.SqlQuery",
    "org.jdbi.v3.sqlobject.statement.SqlScript",
    "org.jdbi.v3.sqlobject.statement.SqlCall",
    "org.jdbi.v3.sqlobject.statement.SqlBatch"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "org.jdbi.v3.sqlobject.statement.SqlUpdate")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null

    if (AnnotationUtil.isAnnotated(method, annotations, 0)) {
      return interaction
    }

    return null
  }
}