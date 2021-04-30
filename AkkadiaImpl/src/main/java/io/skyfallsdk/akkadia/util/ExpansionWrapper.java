package io.skyfallsdk.akkadia.util;

import io.skyfallsdk.expansion.Expansion;
import io.skyfallsdk.expansion.ExpansionInfo;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class ExpansionWrapper implements Expansion {

    private final JavaPlugin plugin;

    @SuppressWarnings("unchecked")
    public ExpansionWrapper(JavaPlugin plugin, PluginDescriptionFile file) {
        this.plugin = plugin;

        DynamicExpansionInfo info = new DynamicExpansionInfo(file);
        try {
            Method annotationData = Class.class.getDeclaredMethod("annotationData");
            annotationData.setAccessible(true);

            Object annotationDataClass = annotationData.invoke(this.getClass());

            Field annotations = annotationDataClass.getClass().getDeclaredField("annotations");
            annotations.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(annotations, annotations.getModifiers() & ~Modifier.FINAL);

            annotations.set(annotationDataClass, new HashMap<>() {{
                put(ExpansionInfo.class, info);
            }});
        } catch (NullPointerException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
            getLogger().fatal(e);
        }
    }

    @Override
    public void onStartup() {
        this.plugin.onEnable();
    }

    @Override
    public void onShutdown() {
        this.plugin.onDisable();
    }

    private class DynamicExpansionInfo implements ExpansionInfo {

        private final PluginDescriptionFile file;

        public DynamicExpansionInfo(PluginDescriptionFile file) {
            this.file = file;
        }

        @Override
        public String name() {
            return this.file.getName() + "-Bukkit";
        }

        @Override
        public String version() {
            return this.file.getVersion();
        }

        @Override
        public String[] authors() {
            return this.file.getAuthors().toArray(new String[0]);
        }

        @Override
        public String[] dependencies() {
            return this.file.getDepend().toArray(new String[0]);
        }

        @Override
        public long expansionId() {
            return 0;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return DynamicExpansionInfo.class;
        }
    }
}
