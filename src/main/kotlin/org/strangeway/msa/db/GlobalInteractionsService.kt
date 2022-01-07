package org.strangeway.msa.db

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.intellij.lang.annotations.Language
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.APP)
class GlobalInteractionsService : SimpleModificationTracker() {
  @Volatile
  private var initialized: Boolean = false

  private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  @Language("http-url-reference")
  private val dbUrl: String = "https://msa.strangeway.org/api/suggestions/db"

  @Language("http-url-reference")
  private val dbSuggestionUrl: String = "https://msa.strangeway.org/api/suggestions"

  private val interactionsDb: MutableList<InteractionMapping> = mutableListOf()

  @RequiresEdt
  fun suggestInteractionMapping(mapping: InteractionMapping) {
    lock.write {
      getState().addMapping(mapping)

      interactionsDb.clear()
      initialized = false
    }

    // todo send async request to web service

    incModificationCount()
  }

  @RequiresBackgroundThread
  fun updateDatabase() {
    // todo run HTTP request

    // todo reset caches in project services
    incModificationCount()
  }

  fun getGlobalInteractions(): List<InteractionMapping> {
    return lock.read {
      checkInitialized()

      interactionsDb
    }
  }

  private fun checkInitialized() {
    if (!initialized) {
      lock.readLock().unlock()
      lock.writeLock().lock()

      try {
        if (!initialized) {
          val globalState = getState()
          val interactions = globalState.interactions.asSequence()
            .filter { it.enabled }
            .map { it.toMapping() }

          interactionsDb.clear()
          interactionsDb.addAll(interactions)

          initialized = true
        }
      } finally {
        lock.readLock().lock()
        lock.writeLock().unlock()
      }
    }
  }

  private fun getState(): InteractionsState {
    return InteractionsState.getInstance()
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

fun getGlobalInteractionsService(): GlobalInteractionsService {
  return ApplicationManager.getApplication().getService(GlobalInteractionsService::class.java)
}