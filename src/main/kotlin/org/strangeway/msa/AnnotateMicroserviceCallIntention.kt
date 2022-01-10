package org.strangeway.msa

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScope
import com.intellij.ui.SimpleListCellRenderer
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getUCallExpression
import org.jetbrains.uast.toUElementOfType
import org.strangeway.msa.db.InteractionMapping
import org.strangeway.msa.db.InteractionType
import org.strangeway.msa.db.getGlobalInteractionsService
import org.strangeway.msa.db.getProjectInteractionsService
import javax.swing.Icon
import javax.swing.ListSelectionModel

@Suppress("IntentionDescriptionNotFoundInspection")
class AnnotateMicroserviceCallIntention : IntentionAction, Iconable, LowPriorityAction {
  override fun startInWriteAction(): Boolean = false

  override fun getText(): String = "Mark microservice interaction method"
  override fun getFamilyName(): String = "Microservice annotator"
  override fun getIcon(flags: Int): Icon = MicroserviceIcons.INTENTION

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    if (file == null || editor == null) return false

    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset)

    val uIdentifier = element.toUElementOfType<UIdentifier>() ?: return false
    val uCall = uIdentifier.getUCallExpression(searchLimit = 3) ?: return false
    val callKind = uCall.kind

    if (callKind == UastCallKind.METHOD_CALL && uCall.methodIdentifier?.sourcePsi != uIdentifier.sourcePsi) {
      return false
    }

    return callKind == UastCallKind.METHOD_CALL || callKind == UastCallKind.CONSTRUCTOR_CALL
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    if (file == null || editor == null) return

    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset)

    val identifier = element.toUElementOfType<UIdentifier>()
    val call = identifier.getUCallExpression(searchLimit = 3)
    val resolvedCall = call?.resolve() ?: return

    showTypesPopup(project, editor, file, resolvedCall)
  }

  private fun showTypesPopup(project: Project, editor: Editor, file: PsiFile, resolvedCall: PsiMethod) {
    val renderer = SimpleListCellRenderer.create<InteractionType> { label, value, _ ->
      label.icon = value.icon
      label.text = value.title
    }

    val types = InteractionType.values().sortedBy { it.title }

    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(types)
      .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      .setTitle("Choose Interaction Type")
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
      .setRenderer(renderer)
      .setNamerForFiltering { it.title }
      .setItemChosenCallback { type ->
        WriteCommandAction.writeCommandAction(project)
          .withName("Add Microservice Interaction Mapping")
          .withGlobalUndo()
          .run<Throwable> {
            addMapping(resolvedCall, type, project, file)
          }
      }
      .createPopup()
      .showInBestPositionFor(editor)
  }

  private fun addMapping(
    resolvedCall: PsiMethod,
    type: InteractionType,
    project: Project,
    file: PsiFile
  ) {
    val declarationFile = resolvedCall.containingFile?.virtualFile ?: return
    val className = resolvedCall.containingClass?.qualifiedName ?: return

    val mapping = InteractionMapping(
      JVM_LANGUAGE,
      className,
      resolvedCall.name,
      resolvedCall.parameterList.parametersCount,
      type
    )

    if (ProjectScope.getLibrariesScope(project).contains(declarationFile)) {
      getGlobalInteractionsService().suggestInteractionMapping(mapping)
    } else {
      getProjectInteractionsService(project).suggestInteractionMapping(mapping)
    }

    DaemonCodeAnalyzer.getInstance(project).restart(file)
  }
}