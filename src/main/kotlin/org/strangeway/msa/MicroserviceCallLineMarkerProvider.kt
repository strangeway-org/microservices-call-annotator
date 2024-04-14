package org.strangeway.msa

import com.intellij.codeInsight.daemon.*
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil.getSymbolPresentableText
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Function
import org.jetbrains.uast.*
import org.strangeway.msa.db.getGlobalInteractionsService
import org.strangeway.msa.db.getProjectInteractionsService
import org.strangeway.msa.frameworks.CallDetector
import org.strangeway.msa.frameworks.FrameworkInteraction
import org.strangeway.msa.frameworks.Interaction
import org.strangeway.msa.frameworks.MappedInteraction
import java.awt.event.MouseEvent
import java.util.function.Supplier
import javax.swing.Icon

class MicroserviceCallLineMarkerProvider : LineMarkerProviderDescriptor() {
  override fun getName(): String = "Microservice method calls"

  override fun getIcon(): Icon = MicroserviceIcons.Gutter.CLOUD_STORAGE

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

  override fun collectSlowLineMarkers(
    elements: MutableList<out PsiElement>,
    result: MutableCollection<in LineMarkerInfo<*>>
  ) {
    val psiElement = elements.firstOrNull() ?: return
    val project = psiElement.project

    for (element in elements) {
      val uIdentifier = element.toUElementOfType<UIdentifier>()
      val uCall = uIdentifier?.getUCallExpression(searchLimit = 3)
      val callKind = uCall?.kind

      if (callKind == UastCallKind.METHOD_CALL || callKind == UastCallKind.CONSTRUCTOR_CALL) {
        if (callKind == UastCallKind.METHOD_CALL && uCall.methodIdentifier?.sourcePsi != uIdentifier.sourcePsi) {
          // this is not identifier of call
          continue
        }

        if (callKind == UastCallKind.CONSTRUCTOR_CALL
          && getConstructorIdentifier(uCall)?.sourcePsi != uIdentifier.sourcePsi
        ) {
          // this is not identifier of constructor
          continue
        }

        for (callDetector in CallDetector.getCallDetectors(project)) {
          val interaction = callDetector.getCallInteraction(project, uCall)
          if (interaction != null) {
            val type = interaction.type
            val title = if (interaction is FrameworkInteraction) interaction.framework else type.title

            result.add(
              MsLineMarkerInfo(
                element,
                element.textRange,
                type.icon,
                { title + ": " + getSymbolPresentableText(it) + "()" },
                { title + ": " + getSymbolPresentableText(it) + "()" },
                { e, elt -> showGutterMenu(interaction, e, elt) },
                type.accessibleNameProvider
              )
            )
            break
          }
        }
      }
    }
  }

  // hack IDEA-287566
  private fun getConstructorIdentifier(uCall: UCallExpression): UElement? {
    if (uCall.lang.`is`(JavaLanguage.INSTANCE)) {
      return uCall.classReference?.referenceNameElement
    }
    return uCall.methodIdentifier
  }

  private fun showGutterMenu(interaction: Interaction, e: MouseEvent?, elt: PsiElement?) {
    if (e == null || elt == null) return

    val actionGroup = DefaultActionGroup()

    if (interaction is MappedInteraction) {
      actionGroup.add(object : AnAction(
        "Remove Mapping", "Remove interaction mapping from settings",
        AllIcons.Actions.DeleteTag
      ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
          getGlobalInteractionsService().removeMapping(interaction.mapping)
          getProjectInteractionsService(elt.project).removeMapping(interaction.mapping)

          DaemonCodeAnalyzer.getInstance(elt.project).restart()
        }

        override fun update(e: AnActionEvent) {}
      })
    }

    actionGroup.add(object : AnAction(
      "Submit Feedback", "Send an e-mail to the maintainer",
      AllIcons.General.User
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

      override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse("https://github.com/strangeway-org/microservices-annotator-db/issues/new")
      }

      override fun update(e: AnActionEvent) {}
    })

    JBPopupFactory.getInstance()
      .createActionGroupPopup(
        "Interaction Actions",
        actionGroup,
        Utils.createAsyncDataContext(DataManager.getInstance().getDataContext(e.component)),
        JBPopupFactory.ActionSelectionAid.MNEMONICS,
        false
      )
      .show(RelativePoint(e))
  }

  private class MsLineMarkerInfo(
    element: PsiElement,
    textRange: TextRange,
    icon: Icon,
    tooltipProvider: Function<in PsiElement, String>,
    presentationProvider: Function<PsiElement, String>,
    navHandler: GutterIconNavigationHandler<PsiElement>,
    accessibleNameProvider: Supplier<String>
  ) : MergeableLineMarkerInfo<PsiElement>(
    element,
    textRange,
    icon,
    tooltipProvider,
    presentationProvider,
    navHandler,
    GutterIconRenderer.Alignment.LEFT,
    accessibleNameProvider
  ) {
    override fun canMergeWith(info: MergeableLineMarkerInfo<*>): Boolean {
      return info is MsLineMarkerInfo
    }

    override fun getCommonIcon(infos: MutableList<out MergeableLineMarkerInfo<*>>): Icon {
      if (infos.all { it.icon == icon }) return icon

      return MicroserviceIcons.Gutter.MERGED
    }
  }
}