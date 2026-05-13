package com.zeros.scriptassistant

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(name = "DeviceAliasConfig", storages = [Storage("deviceAliasConfig.xml")])
class DeviceAliasConfig : PersistentStateComponent<DeviceAliasConfig?> {
    var deviceAliases: MutableMap<String?, String?> = HashMap<String?, String?>()

    override fun getState(): DeviceAliasConfig {
        return this
    }

    override fun loadState(state: DeviceAliasConfig) {
        XmlSerializerUtil.copyBean<DeviceAliasConfig>(state, this)
    }

    fun getAlias(serial: String?): String? {
        return deviceAliases.getOrDefault(serial, "d")
    }

    fun setAlias(serial: String?, alias: String?) {
        deviceAliases[serial] = alias
    }

    var pythonInterpreterPath: String?
        get() = deviceAliases["pythonInterpreterPath"]
        set(path) {
            deviceAliases["pythonInterpreterPath"] = path
        }

    companion object {
        fun getInstance(project: Project): DeviceAliasConfig? {
            return project.getService<DeviceAliasConfig?>(DeviceAliasConfig::class.java)
        }
    }
}
