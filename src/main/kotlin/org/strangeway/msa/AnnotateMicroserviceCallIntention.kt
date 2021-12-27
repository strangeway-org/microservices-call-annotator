package org.strangeway.msa

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import org.jetbrains.uast.*
import javax.swing.Icon

@Suppress("IntentionDescriptionNotFoundInspection")
internal class AnnotateMicroserviceCallIntention : IntentionAction, Iconable, LowPriorityAction {
  override fun startInWriteAction(): Boolean = false

  override fun getText(): String = "Mark microservice interaction method"
  override fun getFamilyName(): String = "Microservice annotator"
  override fun getIcon(flags: Int): Icon = MicroserviceIcons.INTENTION

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    if (file == null || editor == null) return false

    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset)

    val identifier = element.toUElementOfType<UIdentifier>()
    val call = identifier.getUCallExpression(searchLimit = 2)
    return call != null
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (file == null || editor == null) return


  }
}