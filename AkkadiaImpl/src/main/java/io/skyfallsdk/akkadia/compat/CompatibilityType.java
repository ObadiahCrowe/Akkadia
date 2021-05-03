package io.skyfallsdk.akkadia.compat;

import com.google.common.collect.Maps;
import io.skyfallsdk.Server;
import io.skyfallsdk.akkadia.Akkadia;
import io.skyfallsdk.akkadia.compat.type.EssentialsXCompatibility;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum CompatibilityType {

    ESSENTIALS(EssentialsXCompatibility.class, () -> Server.get().getExpansion(Akkadia.class).getConfig().getCompatibility().usingEssentialsCompat(), name -> {
        return name.startsWith("com.earth2me.essentials");
    })

    ;

    private static final Map<Class<? extends AbstractCompatibilityWrapper>, AbstractCompatibilityWrapper> INSTANCES = Maps.newHashMap();

    static {
        for (CompatibilityType type : CompatibilityType.values()) {
            try {
                AbstractCompatibilityWrapper wrapper = type.wrapper.getConstructor().newInstance();

                INSTANCES.put(type.wrapper, wrapper);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private final String name;
    private final Class<? extends AbstractCompatibilityWrapper> wrapper;
    private final Supplier<Boolean> isEnabled;
    private final Function<String, Boolean> matchesPackageName;

    CompatibilityType(Class<? extends AbstractCompatibilityWrapper> wrapper, Supplier<Boolean> isEnabled, Function<String, Boolean> matchesPackageName) {
        this.name = this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
        this.wrapper = wrapper;
        this.isEnabled = isEnabled;
        this.matchesPackageName = matchesPackageName;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.isEnabled.get();
    }

    public AbstractCompatibilityWrapper getWrapper() {
        return INSTANCES.getOrDefault(this.wrapper, null);
    }

    public boolean matchesPackageName(String name) {
        return this.matchesPackageName.apply(name);
    }
}
