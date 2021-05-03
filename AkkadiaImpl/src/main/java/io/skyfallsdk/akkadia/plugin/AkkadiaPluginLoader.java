package io.skyfallsdk.akkadia.plugin;

import com.google.common.collect.Maps;
import io.skyfallsdk.akkadia.Akkadia;
import io.skyfallsdk.akkadia.compat.CompatibilityType;
import io.skyfallsdk.akkadia.logger.AkkadiaPluginLogger;
import io.skyfallsdk.akkadia.util.ExpansionWrapper;
import io.skyfallsdk.expansion.Expansion;
import io.skyfallsdk.expansion.ExpansionInfo;
import io.skyfallsdk.expansion.ServerExpansionRegistry;
import javassist.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class AkkadiaPluginLoader implements PluginLoader {

    public static final Map<Class<? extends JavaPlugin>, Class<? extends Expansion>> EXPANSION_CLASSES = Maps.newConcurrentMap();

    private final Pattern[] fileFilters = new Pattern[]{Pattern.compile("\\.jar$")};
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>();
    private final List<BukkitPluginClassLoader> loaders = new CopyOnWriteArrayList<BukkitPluginClassLoader>();

    final Server server;

    private final Path pluginDir;
    private final ClassPool pool;

    public AkkadiaPluginLoader(Server server) {
        this.server = server;

        this.pool = ClassPool.getDefault();
        this.pool.appendClassPath(new ClassClassPath(ExpansionWrapper.class));
        this.pool.appendClassPath(new ClassClassPath(PluginBase.class));
        this.pool.appendClassPath(new ClassClassPath(JavaPlugin.class));
        this.pool.appendClassPath(new ClassClassPath(PluginDescriptionFile.class));

        io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().info("Overwriting JavaPlugin constructor to enable Bukkit plugin compatibility.");
/*        try {
            CtClass ctClass = this.pool.get("org.bukkit.plugin.java.JavaPlugin");
            CtConstructor ctConstructor = ctClass.getConstructors()[0];

            ctConstructor.setBody("{ return; }");

            ctClass.toClass(JavaPlugin.class);
        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        }*/

        io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().info("Attempting to find Bukkit plugins.");
        this.pluginDir = io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getPath().resolve("plugins");
        if (!Files.exists(this.pluginDir)) {
            try {
                Files.createDirectory(this.pluginDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.list(this.pluginDir).forEachOrdered(dir -> {
                if (Files.isDirectory(dir)) {
                    return;
                }

                if (!dir.getFileName().toString().endsWith(".jar")) {
                    return;
                }

                try {
                    io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().info("Found Bukkit plugin at: " + dir.getFileName());
                    AkkadiaPluginLoader.this.loadPlugin(dir.toFile());
                } catch (InvalidPluginException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().error(e);
        }
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        final PluginDescriptionFile description;
        try {
            description = getPluginDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidPluginException(ex);
        }

        io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().info("Found Bukkit plugin: " + description.getName() + ":" + description.getVersion());

        Path pluginDataFolder = this.pluginDir.resolve(description.getName());
        if (!Files.exists(pluginDataFolder)) {
            try {
                Files.createDirectory(pluginDataFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final BukkitPluginClassLoader loader;
        try {
            loader = new BukkitPluginClassLoader(this, getClass().getClassLoader(), description, pluginDataFolder.toFile(), file);
        } catch (InvalidPluginException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InvalidPluginException(ex);
        }

        try {
            Method initMethod = JavaPlugin.class.getDeclaredMethod("init", PluginLoader.class, Server.class, PluginDescriptionFile.class, File.class, File.class, ClassLoader.class);
            initMethod.setAccessible(true);

            initMethod.invoke(loader.plugin, this, this.server, description, pluginDataFolder.toFile(), file, loader);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new InvalidPluginException("Could not initialise JavaPlugin", e);
        }

        try {
            Field field = JavaPlugin.class.getDeclaredField("logger");
            field.setAccessible(true);

            field.set(loader.plugin, new AkkadiaPluginLogger(loader.plugin));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new InvalidPluginException("Could not set logger", e);
        }

        loaders.add(loader);

        Class<? extends ExpansionWrapper> wrapperClass = null;

        try {
            CtClass expansionWrapper = this.pool.get("io.skyfallsdk.akkadia.util.ExpansionWrapper");
            CtClass pluginExpansion = this.pool.makeClass(loader.plugin.getClass().getPackageName() + '.' + description.getName() + "Expansion", expansionWrapper);

            pluginExpansion.addConstructor(CtNewConstructor.make(new CtClass[] {
              this.pool.get("org.bukkit.plugin.java.JavaPlugin"),
              this.pool.get("org.bukkit.plugin.PluginDescriptionFile")
            }, null, CtNewConstructor.PASS_PARAMS, null, null, pluginExpansion));

            wrapperClass = (Class<? extends ExpansionWrapper>) pluginExpansion.toClass(loader);
        } catch (Exception e) {
            io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().error(e);
        }

        ServerExpansionRegistry registry = (ServerExpansionRegistry) io.skyfallsdk.Server.get().getExpansionRegistry();
        try {
            Field instanceField = ServerExpansionRegistry.class.getDeclaredField("EXPANSION_INSTANCES");
            instanceField.setAccessible(true);

            Field infoField = ServerExpansionRegistry.class.getDeclaredField("EXPANSION_INFO");
            infoField.setAccessible(true);

            Field modifierField = Field.class.getDeclaredField("modifiers");
            modifierField.setAccessible(true);

            modifierField.set(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);
            modifierField.set(infoField, infoField.getModifiers() & ~Modifier.FINAL);

            ExpansionWrapper wrapper = wrapperClass == null ? new ExpansionWrapper(loader.plugin, description) :
              wrapperClass.getConstructor(JavaPlugin.class, PluginDescriptionFile.class).newInstance(loader.plugin, description);
            ExpansionInfo annotation = wrapper.getClass().getAnnotation(ExpansionInfo.class);

            synchronized (this) {
                Map<Class<? extends Expansion>, Expansion> instances = (Map<Class<? extends Expansion>, Expansion>) instanceField.get(null);
                Map<Class<? extends Expansion>, ExpansionInfo> info = (Map<Class<? extends Expansion>, ExpansionInfo>) infoField.get(null);

                instances.put(wrapper.getClass(), wrapper);
                info.put(wrapper.getClass(), annotation);

                instanceField.set(null, instances);
                infoField.set(null, info);
            }

            registry.getLocalThread(wrapper).start();
            io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().info("Successfully loaded Bukkit Plugin, " + annotation.name() + "!");
        } catch (NoSuchFieldException | NullPointerException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            io.skyfallsdk.Server.get().getExpansion(Akkadia.class).getLogger().error(e);
        }

        return loader.plugin;
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("plugin.yml");

            if (entry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain plugin.yml"));
            }

            stream = jar.getInputStream(entry);

            return new PluginDescriptionFile(stream);
        } catch (IOException | YAMLException ex) {
            throw new InvalidDescriptionException(ex);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {}
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {}
            }
        }
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return fileFilters.clone();
    }

    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (BukkitPluginClassLoader loader : loaders) {
                try {
                    cachedClass = loader.findClass(name, false);
                } catch (ClassNotFoundException cnfe) {}
                if (cachedClass != null) {
                    return cachedClass;
                }
            }
        }
        return null;
    }

    void setClass(final String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name, clazz);

            if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass(ConfigurationSerializable.class);
                ConfigurationSerialization.registerClass(serializable);
            }
        }
    }

    private void removeClass(String name) {
        Class<?> clazz = classes.remove(name);

        try {
            if ((clazz != null) && (ConfigurationSerializable.class.isAssignableFrom(clazz))) {
                Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass(ConfigurationSerializable.class);
                ConfigurationSerialization.unregisterClass(serializable);
            }
        } catch (NullPointerException ex) {
            // Boggle!
            // (Native methods throwing NPEs is not fun when you can't stop it before-hand)
        }
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return null;
    }

    @Override
    public void enablePlugin(Plugin plugin) {

    }

    @Override
    public void disablePlugin(Plugin plugin) {

    }
}
