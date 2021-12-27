package org.strangeway.msa

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MicroserviceIcons {
  val INTENTION: Icon = getIcon("/org/strangeway/msa/intention.svg")

  object Gutter {
    val DATABASE: Icon = getIcon("/org/strangeway/msa/gutter/database.svg")
    val FILESYSTEM: Icon = getIcon("/org/strangeway/msa/gutter/filesystem.svg")
    val IO_READ: Icon = getIcon("/org/strangeway/msa/gutter/io-read.svg")
    val IO_WRITE: Icon = getIcon("/org/strangeway/msa/gutter/io-write.svg")
    val MESSAGE_SEND: Icon = getIcon("/org/strangeway/msa/gutter/message-send.svg")
    val MESSAGE_RECEIVE: Icon = getIcon("/org/strangeway/msa/gutter/message-receive.svg")
    val BROADCAST: Icon = getIcon("/org/strangeway/msa/gutter/broadcast.svg")
    val CLOUD_STORAGE: Icon = getIcon("/org/strangeway/msa/gutter/cloud-storage.svg")
    val REQUEST: Icon = getIcon("/org/strangeway/msa/gutter/request.svg")
    val RUN_PROCESS: Icon = getIcon("/org/strangeway/msa/gutter/run-process.svg")
    val WEBSOCKET: Icon = getIcon("/org/strangeway/msa/gutter/websocket.svg")
    val STREAMING: Icon = getIcon("/org/strangeway/msa/gutter/streaming.svg")
  }

  @JvmStatic
  private fun getIcon(iconPath: String): Icon = IconLoader.getIcon(iconPath, MicroserviceIcons::class.java)
}