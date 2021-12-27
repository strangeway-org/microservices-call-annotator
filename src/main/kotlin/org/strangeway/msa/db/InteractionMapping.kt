package org.strangeway.msa.db

data class InteractionMapping(
  val className: String,
  val methodName: String,
  val argsCount: Int,
  val type: InteractionType
)