package org.strangeway.msa.db

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

open class InteractionsState : PersistentStateComponent<InteractionsState> {
  var interactions: MutableList<InteractionState> = ArrayList()
  var lastUpdateTs: Long = Long.MIN_VALUE

  override fun getState(): InteractionsState = this

  override fun loadState(state: InteractionsState) {
    this.interactions.clear()
    this.interactions.addAll(state.interactions)

    this.lastUpdateTs = state.lastUpdateTs
  }

  fun addMapping(mapping: InteractionMapping) {
    if (interactions.any { equivalent(it, mapping) }) {
      return
    }

    interactions.add(mapping.toState())
  }

  fun removeMapping(mapping: InteractionMapping) {
    val interactionState = interactions.find { equivalent(it, mapping) }
    if (interactionState != null) {
      if (interactionState.shared) {
        interactionState.enabled = false
      } else {
        interactions.remove(interactionState)
      }
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(): InteractionsState {
      return ApplicationManager.getApplication().getService(GlobalInteractionsState::class.java)
    }

    @JvmStatic
    fun getInstance(project: Project): InteractionsState {
      return project.getService(ProjectInteractionsState::class.java)
    }
  }
}

@State(name = "GlobalInteractionsState", storages = [Storage("microserviceCalls.xml")])
class GlobalInteractionsState : InteractionsState()

@State(name = "ProjectInteractionsState", storages = [Storage("microserviceCalls.xml")])
class ProjectInteractionsState : InteractionsState()

open class InteractionState {
  var language: String = ""
  var className: String = ""
  var methodName: String = ""
  var argsCount: Int = 0
  var type: InteractionType = InteractionType.REQUEST

  var shared: Boolean = false
  var enabled: Boolean = true
}

fun equivalent(it: InteractionState, mapping: InteractionMapping): Boolean {
  return it.className == mapping.className && it.methodName == mapping.methodName && it.argsCount == mapping.argsCount
}

fun InteractionState.toMapping(): InteractionMapping {
  return InteractionMapping(language, className, methodName, argsCount, type)
}

fun InteractionMapping.toState(): InteractionState {
  val mapping = this
  return InteractionState().apply {
    language = mapping.language
    className = mapping.className
    methodName = mapping.methodName
    argsCount = mapping.argsCount
    type = mapping.type
  }
}