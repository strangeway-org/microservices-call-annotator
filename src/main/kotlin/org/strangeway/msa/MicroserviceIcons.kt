package org.strangeway.msa

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MicroserviceIcons {
  val INTENTION: Icon = IconLoader.getIcon("/org/strangeway/msa/intention.svg", MicroserviceIcons::class.java)

  val DATABASE: Icon = IconLoader.getIcon("/org/strangeway/msa/database.svg", MicroserviceIcons::class.java)
  val FILESYSTEM: Icon = IconLoader.getIcon("/org/strangeway/msa/filesystem.svg", MicroserviceIcons::class.java)
  val IO_STREAM: Icon = IconLoader.getIcon("/org/strangeway/msa/io-stream.svg", MicroserviceIcons::class.java)
  val MESSAGE: Icon = IconLoader.getIcon("/org/strangeway/msa/message.svg", MicroserviceIcons::class.java)
  val NETWORK: Icon = IconLoader.getIcon("/org/strangeway/msa/network.svg", MicroserviceIcons::class.java)
  val RUN_PROCESS: Icon = IconLoader.getIcon("/org/strangeway/msa/run-process.svg", MicroserviceIcons::class.java)
  val WEBSOCKET: Icon = IconLoader.getIcon("/org/strangeway/msa/websocket.svg", MicroserviceIcons::class.java)
}