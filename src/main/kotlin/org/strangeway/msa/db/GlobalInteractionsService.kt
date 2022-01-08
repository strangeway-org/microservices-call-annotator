package org.strangeway.msa.db

import com.google.gson.Gson
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.HttpRequests
import org.intellij.lang.annotations.Language
import java.io.File
import java.net.HttpURLConnection
import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.APP)
class GlobalInteractionsService : SimpleModificationTracker() {
  private val log: Logger = Logger.getInstance(GlobalInteractionsService::class.java)

  @Volatile
  private var initialized: Boolean = false

  private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

  @Language("http-url-reference")
  private val dbUrl: String = "https://msa.strangeway.org/api/suggestions/db"
  @Language("http-url-reference")
  private val fallbackDbUrl: String = "https://raw.githubusercontent.com/strangeway-org/microservices-annotator-db/main/db.json"

  @Language("http-url-reference")
  private val dbSuggestionUrl: String = "https://msa.strangeway.org/api/suggestions"

  private val updateDelay: Duration = Duration.ofDays(1)

  private val interactionsDb: MutableList<InteractionMapping> = mutableListOf()

  fun suggestInteractionMapping(mapping: InteractionMapping) {
    lock.write {
      getState().addMapping(mapping)

      interactionsDb.clear()
      initialized = false
    }

    AppExecutorUtil.getAppExecutorService().submit {
      suggestForPublicDb(mapping)
    }

    incModificationCount()
  }

  private fun suggestForPublicDb(mapping: InteractionMapping) {
    if (SHARE_JAVA_PACKAGE_PREFIXES.none { mapping.className.startsWith(it) }) {
      log.info("Interaction mapping is not allowed to share to public " + mapping.className)
      return
    }

    val suggestion = DbSuggestion()

    suggestion.language = mapping.language
    suggestion.className = mapping.className
    suggestion.methodName = mapping.methodName
    suggestion.argsCount = mapping.argsCount
    suggestion.interactionType = mapping.interactionType

    val payload = Gson().toJson(suggestion)

    try {
      HttpRequests.post(dbSuggestionUrl, "application/json")
        .productNameAsUserAgent()
        .throwStatusCodeException(false)
        .connect {
          it.write(payload)

          val responseCode = (it.connection as? HttpURLConnection)?.responseCode ?: 0
          if (responseCode == 200 || responseCode == 202) {
            log.info("Suggested new mapping to microservice annotator db")
          } else {
            val error = it.readString()
            println(error)

            log.warn("Unable to suggest new mapping to microservice annotator db: $responseCode")
          }
        }
    } catch (e: Exception) {
      log.warn("Unable to suggest new mapping to microservice annotator db", e)
    }
  }

  fun removeMapping(mapping: InteractionMapping) {
    lock.write {
      getState().removeMapping(mapping)

      interactionsDb.clear()
      initialized = false
    }

    incModificationCount()
  }

  @RequiresBackgroundThread
  fun updateDatabase(project: Project, progressIndicator: ProgressIndicator, force: Boolean) {
    if (!force) {
      val lastUpdateTs = Instant.ofEpochMilli(getState().lastUpdateTs)
      val now = Instant.now()

      if (now.minus(updateDelay).isBefore(lastUpdateTs)) {
        log.debug("Too early to update microservice annotator db")
        return
      }
    }

    log.debug("Updating microservice all annotator db")

    try {
      val tempFile = FileUtil.createTempFile("microservice-annotator-db", ".json")

      HttpRequests.request(dbUrl)
        .productNameAsUserAgent()
        .throwStatusCodeException(false)
        .connect {
          val responseCode = (it.connection as? HttpURLConnection)?.responseCode ?: 0

          if (responseCode == 200) {
            handleDbResponse(it, tempFile, progressIndicator, project, force)
          } else {
            log.warn("Unable to update microservice annotator db: $responseCode")

            updateDatabaseWithFallbackUrl(project, tempFile, progressIndicator, force)
          }
        }
    } catch (e: Exception) {
      log.warn("Unable to update microservice annotator db", e)
    }
  }

  private fun updateDatabaseWithFallbackUrl(
    project: Project,
    tempFile: File,
    progressIndicator: ProgressIndicator,
    force: Boolean
  ) {
    HttpRequests.request(fallbackDbUrl)
      .productNameAsUserAgent()
      .throwStatusCodeException(false)
      .connect {
        val responseCode = (it.connection as? HttpURLConnection)?.responseCode ?: 0

        if (responseCode == 200) {
          handleDbResponse(it, tempFile, progressIndicator, project, force)
        } else {
          log.warn("Unable to update microservice annotator db with fallback URL: $responseCode")
        }
      }
  }

  private fun handleDbResponse(
    it: HttpRequests.Request,
    tempFile: File,
    progressIndicator: ProgressIndicator,
    project: Project,
    force: Boolean
  ) {
    it.saveToFile(tempFile, progressIndicator)
    val changed = updateDatabaseState(tempFile)

    if (changed > 0) {
      notifyUserDbUpdated(changed, project)

      if (force) {
        // apply immediately
        DaemonCodeAnalyzer.getInstance(project).restart()
      }
    } else if (force) {
      notifyUserNoUpdates(project)
    }

    log.info("Updated microservice annotator db")
  }

  private fun notifyUserDbUpdated(changed: Int, project: Project) {
    ApplicationManager.getApplication().invokeLater({
      Notification(
        "MicroservicesCallAnnotator",
        "Interactions updated",
        "Microservices annotator database updated. $changed new records",
        NotificationType.INFORMATION
      ).notify(project)
    }, ModalityState.NON_MODAL, project.disposed)
  }

  private fun notifyUserNoUpdates(project: Project) {
    ApplicationManager.getApplication().invokeLater({
      Notification(
        "MicroservicesCallAnnotator",
        "No interaction updates",
        "Microservices annotator database not changed yet",
        NotificationType.INFORMATION
      ).notify(project)
    }, ModalityState.NON_MODAL, project.disposed)
  }

  private fun updateDatabaseState(tempFile: File): Int {
    val response = tempFile.bufferedReader().use {
      Gson().fromJson(it, DbResponse::class.java)
    }

    val dbInteractions = response.interactions.map {
      val state = InteractionState()

      state.shared = true
      state.language = it.language
      state.className = it.className
      state.methodName = it.methodName
      state.argsCount = it.argsCount
      state.interactionType = it.interactionType

      state
    }

    val state = getState()

    val changedSize = lock.write {
      // merge states
      val newInteractions = state.interactions.toMutableList()
      val sizeBefore = newInteractions.size

      val disabled = newInteractions.filter { it.shared && !it.enabled }

      newInteractions.removeIf { it.shared }
      newInteractions.addAll(dbInteractions)

      for (interaction in disabled) {
        newInteractions.find { equivalent(interaction, it) }?.enabled = false
      }

      state.interactions = newInteractions
      state.lastUpdateTs = Instant.now().toEpochMilli()

      val sizeAfter = newInteractions.size

      initialized = false

      sizeAfter - sizeBefore
    }

    incModificationCount()

    return changedSize
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

  fun reset() {
    lock.write {
      getState().interactions = mutableListOf()
      getState().lastUpdateTs = Long.MIN_VALUE

      initialized = false
    }

    incModificationCount()
  }

  class DbResponse {
    var interactions: MutableList<DbInteractionMapping> = mutableListOf()
  }

  class DbInteractionMapping {
    var language: String = ""
    var className: String = ""
    var methodName: String = ""
    var argsCount: Int = 0
    var interactionType: InteractionType = InteractionType.REQUEST
  }

  class DbSuggestion {
    var language: String = ""
    var className: String = ""
    var methodName: String = ""
    var argsCount: Int = 0
    var interactionType: InteractionType = InteractionType.REQUEST
  }
}

fun getGlobalInteractionsService(): GlobalInteractionsService {
  return ApplicationManager.getApplication().getService(GlobalInteractionsService::class.java)
}