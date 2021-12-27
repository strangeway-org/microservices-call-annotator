package org.strangeway.msa.db

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

@Service(Service.Level.APP)
class DatabaseClient {
  private val dbUrl: String = ""
  private val dbSuggestionUrl: String = ""

  @RequiresBackgroundThread
  fun suggestInteractionMapping(mapping: InteractionMapping) {
    if (SHARE_JAVA_PACKAGE_PREFIXES.none { mapping.className.startsWith(it) }) return


  }

  @RequiresBackgroundThread
  fun fetchDatabase(): List<InteractionMapping> {
    return emptyList()
  }
}

fun getDatabaseClient(): DatabaseClient {
  return ApplicationManager.getApplication().getService(DatabaseClient::class.java)
}