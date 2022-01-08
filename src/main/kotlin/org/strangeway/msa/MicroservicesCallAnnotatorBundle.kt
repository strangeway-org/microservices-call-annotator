package org.strangeway.msa

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val PATH_TO_BUNDLE = "messages.MicroservicesCallAnnotatorBundle"

internal object MicroservicesCallAnnotatorBundle : DynamicBundle(PATH_TO_BUNDLE) {
  @Nls
  @JvmStatic
  fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

  @JvmStatic
  fun messagePointer(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): Supplier<@Nls String> {
    return getLazyMessage(key, *params)
  }
}