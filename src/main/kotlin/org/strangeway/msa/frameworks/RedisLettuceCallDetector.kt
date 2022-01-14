package org.strangeway.msa.frameworks

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.strangeway.msa.db.InteractionType

class RedisLettuceCallDetector : CallDetector {
  private val interaction: Interaction = FrameworkInteraction(InteractionType.DATABASE, "Redis Client")

  private val asyncApis: Set<String> = setOf(
    "io.lettuce.core.api.async.BaseRedisAsyncCommands",
    "io.lettuce.core.api.async.RedisAsyncCommands",
    "io.lettuce.core.api.async.RedisGeoAsyncCommands",
    "io.lettuce.core.api.async.RedisHashAsyncCommands",
    "io.lettuce.core.api.async.RedisHLLAsyncCommands",
    "io.lettuce.core.api.async.RedisKeyAsyncCommands",
    "io.lettuce.core.api.async.RedisListAsyncCommands",
    "io.lettuce.core.api.async.RedisScriptingAsyncCommands",
    "io.lettuce.core.api.async.RedisServerAsyncCommands",
    "io.lettuce.core.api.async.RedisSetAsyncCommands",
    "io.lettuce.core.api.async.RedisSortedSetAsyncCommands",
    "io.lettuce.core.api.async.RedisStreamAsyncCommands",
    "io.lettuce.core.api.async.RedisStringAsyncCommands",
    "io.lettuce.core.api.async.RedisTransactionalAsyncCommands"
  )

  private val reactiveApis: Set<String> = setOf(
    "io.lettuce.core.api.reactive.BaseRedisReactiveCommands",
    "io.lettuce.core.api.reactive.RedisGeoReactiveCommands",
    "io.lettuce.core.api.reactive.RedisHashReactiveCommands",
    "io.lettuce.core.api.reactive.RedisHLLReactiveCommands",
    "io.lettuce.core.api.reactive.RedisKeyReactiveCommands",
    "io.lettuce.core.api.reactive.RedisListReactiveCommands",
    "io.lettuce.core.api.reactive.RedisReactiveCommands",
    "io.lettuce.core.api.reactive.RedisScriptingReactiveCommands",
    "io.lettuce.core.api.reactive.RedisServerReactiveCommands",
    "io.lettuce.core.api.reactive.RedisSetReactiveCommands",
    "io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands",
    "io.lettuce.core.api.reactive.RedisStreamReactiveCommands",
    "io.lettuce.core.api.reactive.RedisStringReactiveCommands",
    "io.lettuce.core.api.reactive.RedisTransactionalReactiveCommands"
  )

  override fun isAvailable(project: Project): Boolean {
    return hasLibraryClass(project, "io.lettuce.core.RedisFuture")
  }

  override fun getCallInteraction(project: Project, uCall: UCallExpression): Interaction? {
    val method = uCall.resolve() ?: return null
    val clazz = method.containingClass ?: return null
    val qualifiedName = clazz.qualifiedName

    if (asyncApis.contains(qualifiedName)) {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null && returnClass.qualifiedName == "io.lettuce.core.RedisFuture") {
        return interaction
      }
    }

    if (reactiveApis.contains(qualifiedName)) {
      val returnClass = (method.returnType as? PsiClassType)?.rawType()?.resolve()
      if (returnClass != null
        && (returnClass.qualifiedName == "org.reactivestreams.Publisher"
            || InheritanceUtil.isInheritor(returnClass, "org.reactivestreams.Publisher"))
      ) {
        return interaction
      }
    }

    return null
  }
}