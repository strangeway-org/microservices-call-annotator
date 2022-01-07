package org.strangeway.msa

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.Utils
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.getUCallExpression
import org.jetbrains.uast.toUElementOfType
import org.strangeway.msa.db.getGlobalInteractionsService
import org.strangeway.msa.db.getProjectInteractionsService
import org.strangeway.msa.frameworks.CallDetector
import org.strangeway.msa.frameworks.Interaction
import java.awt.event.MouseEvent
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
      val uCall = uIdentifier?.getUCallExpression(searchLimit = 2)
      val callKind = uCall?.kind
      if (callKind == UastCallKind.METHOD_CALL || callKind == UastCallKind.CONSTRUCTOR_CALL) {
        for (callDetector in CallDetector.getCallDetectors(project)) {
          val interaction = callDetector.getCallInteraction(project, uCall)
          if (interaction != null) {
            result.add(
              LineMarkerInfo(
                element,
                element.textRange,
                interaction.type.icon,
                interaction.type.tooltip,
                { e, elt -> showGutterMenu(interaction, e, elt) },
                GutterIconRenderer.Alignment.LEFT,
                interaction.type.accessibleNameProvider
              )
            )
            break
          }
        }
      }
    }
  }

  private fun showGutterMenu(interaction: Interaction, e: MouseEvent?, elt: PsiElement?) {
    if (e == null || elt == null) return

    val actionGroup = DefaultActionGroup()

    if (interaction.mapping != null) {
      actionGroup.add(object : AnAction(
        "Remove Mapping", "Remove interaction mapping from settings",
        AllIcons.Actions.DeleteTag
      ) {
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
      override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse("mailto:github@strangeway.org")
      }

      override fun update(e: AnActionEvent) {}
    })

    JBPopupFactory.getInstance()
      .createActionGroupPopup(
        "Interaction Actions",
        actionGroup,
        Utils.wrapToAsyncDataContext(DataManager.getInstance().getDataContext(e.component)),
        JBPopupFactory.ActionSelectionAid.MNEMONICS,
        false
      )
      .show(RelativePoint(e))
  }
}