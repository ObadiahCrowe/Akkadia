package io.skyfallsdk.akkadia.compat.type;

import io.skyfallsdk.akkadia.compat.AbstractCompatibilityWrapper;
import io.skyfallsdk.akkadia.compat.CompatibilityType;
import io.skyfallsdk.akkadia.plugin.BukkitPluginClassLoader;
import net.minecraft.server.v1_16_R3.MinecraftServer;

import java.lang.reflect.Field;

public class EssentialsXCompatibility extends AbstractCompatibilityWrapper {

    public EssentialsXCompatibility() {
        super(CompatibilityType.ESSENTIALS);
    }

    @Override
    public void onAkkadiaLoad() {}

    @Override
    public void onPluginEnable(BukkitPluginClassLoader loader) {
        try {
            Class reflUtil = loader.findClass("net.ess3.nms.refl.ReflUtil");
            Field nmsVersion = reflUtil.getDeclaredField("nmsVersion");
            nmsVersion.setAccessible(true);

            String name = MinecraftServer.class.getName();
            String[] parts = name.split("\\.");

            nmsVersion.set(null, parts[3]);
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPluginDisable() {}
}
