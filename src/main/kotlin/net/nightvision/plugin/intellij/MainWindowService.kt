package net.nightvision.plugin.intellij

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class MainWindowService {
    var windowFactory: MainWindowFactory? = null
}