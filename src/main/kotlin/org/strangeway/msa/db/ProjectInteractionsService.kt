package org.strangeway.msa.db

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.PROJECT)
class ProjectInteractionsService(private val project: Project) : SimpleModificationTracker() {
  @Volatile
  private var initialized: Boolean = false

  private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  private val interactionsDb: MutableList<InteractionMapping> = mutableListOf()

  @RequiresEdt
  fun suggestInteractionMapping(mapping: InteractionMapping) {
    lock.write {
      getState().addMapping(mapping)

      interactionsDb.clear()
      initialized = false
    }

    incModificationCount()
  }

  fun getProjectInteractions(): List<InteractionMapping> {
    return lock.read {
      checkInitialized()

      interactionsDb
    }
  }

  fun hasMethods(methodName: String?): Boolean {
    if (methodName == null) return false

    return getInteractionsIndex(project).containsKey(methodName)
  }

  fun findInteraction(className: String, methodName: String, argsCount: Int): InteractionMapping? {
    val possibleCalls = getInteractionsIndex(project)[methodName]

    return possibleCalls
      ?.find { it.className == className && it.argsCount == argsCount }
  }

  private fun checkInitialized() {
    if (!initialized) {
      lock.readLock().unlock()
      lock.writeLock().lock()

      try {
        val interactions = getState().interactions

        interactionsDb.clear()
        interactionsDb.addAll(interactions.map { it.toMapping() })

        initialized = true
      } finally {
        lock.readLock().lock()
        lock.writeLock().unlock()
      }
    }
  }

  private fun getState(): InteractionsState {
    return InteractionsState.getInstance(project)
  }

  fun removeMapping(mapping: InteractionMapping) {
    lock.write {
      getState().removeMapping(mapping)

      interactionsDb.clear()
      initialized = false
    }

    incModificationCount()
  }
}

private fun getInteractionsIndex(project: Project): Map<String, List<InteractionMapping>> {
  return CachedValuesManager.getManager(project).getCachedValue(project) {
    val javaPsiFacade = JavaPsiFacade.getInstance(project)
    val scope = GlobalSearchScope.allScope(project)
    val globalInteractionsService = getGlobalInteractionsService()
    val projectInteractionsService = getProjectInteractionsService(project)

    val allRecords =
      (projectInteractionsService.getProjectInteractions() + globalInteractionsService.getGlobalInteractions())
        .filter { javaPsiFacade.findClass(it.className, scope) != null }
        .groupBy { it.methodName }

    Result.create(
      allRecords,
      ProjectRootManager.getInstance(project),
      projectInteractionsService,
      globalInteractionsService
    )
  }
}

fun getProjectInteractionsService(project: Project): ProjectInteractionsService {
  return project.service()
}