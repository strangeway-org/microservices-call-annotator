package org.strangeway.msa.db

import org.strangeway.msa.MicroserviceIcons.Gutter
import javax.swing.Icon

enum class InteractionType(val title: String, val icon: Icon) {
  BROADCAST("Broadcast Data Transmission", Gutter.BROADCAST),
  CLOUD_STORAGE("Cloud Storage Access", Gutter.CLOUD_STORAGE),
  DATABASE("Database Access", Gutter.DATABASE),
  FILESYSTEM("Filesystem Operations", Gutter.FILESYSTEM),
  IO_READ("IO Read", Gutter.IO_READ),
  IO_WRITE("IO Read", Gutter.IO_WRITE),
  MESSAGE_RECEIVE("Message Receive", Gutter.MESSAGE_RECEIVE),
  MESSAGE_SEND("Message Send", Gutter.MESSAGE_SEND),
  REQUEST("Network Request", Gutter.REQUEST),
  RUN_PROCESS("External Process", Gutter.RUN_PROCESS),
  WEBSOCKET("WebSocket", Gutter.WEBSOCKET),
  STREAMING("Streaming", Gutter.STREAMING)
}