package io.skyfallsdk.akkadia.compat;

import io.skyfallsdk.akkadia.plugin.BukkitPluginClassLoader;

public abstract class AbstractCompatibilityWrapper {

    private final CompatibilityType type;

    public AbstractCompatibilityWrapper(CompatibilityType type) {
        this.type = type;
    }

    public CompatibilityType getType() {
        return this.type;
    }

    public abstract void onAkkadiaLoad();

    public abstract void onPluginEnable(BukkitPluginClassLoader loader);

    public abstract void onPluginDisable();
}
