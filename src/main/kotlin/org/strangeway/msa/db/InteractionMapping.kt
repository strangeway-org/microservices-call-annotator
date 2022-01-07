package org.strangeway.msa.db

data class InteractionMapping(
  val language: String,
  val className: String,
  val methodName: String,
  val argsCount: Int,
  val type: InteractionType
)