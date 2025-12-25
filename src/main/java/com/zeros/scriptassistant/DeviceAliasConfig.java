package com.zeros.scriptassistant;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(
    name = "DeviceAliasConfig",
    storages = @Storage("deviceAliasConfig.xml")
)
public class DeviceAliasConfig implements PersistentStateComponent<DeviceAliasConfig> {
    public Map<String, String> deviceAliases = new HashMap<>();

    @Nullable
    @Override
    public DeviceAliasConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DeviceAliasConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static DeviceAliasConfig getInstance(Project project) {
        return project.getService(DeviceAliasConfig.class);
    }

    public String getAlias(String serial) {
        return deviceAliases.getOrDefault(serial, "d");
    }

    public void setAlias(String serial, String alias) {
        deviceAliases.put(serial, alias);
    }

    public String getPythonInterpreterPath() {
        return deviceAliases.getOrDefault("pythonInterpreterPath", "");
    }

    public void setPythonInterpreterPath(String path) {
        deviceAliases.put("pythonInterpreterPath", path);
    }
}
