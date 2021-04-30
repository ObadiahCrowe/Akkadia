package io.skyfallsdk.akkadia;

import io.skyfallsdk.Server;
import io.skyfallsdk.akkadia.config.AkkadiaConfig;
import io.skyfallsdk.akkadia.logger.AkkadiaLogger;
import io.skyfallsdk.akkadia.plugin.AkkadiaPluginManager;
import io.skyfallsdk.akkadia.scheduler.AkkadiaScheduler;
import io.skyfallsdk.config.LoadableConfig;
import io.skyfallsdk.expansion.Expansion;
import io.skyfallsdk.expansion.ExpansionInfo;
import org.bukkit.Bukkit;

@ExpansionInfo(name = "Akkadia", version = "1.16.4-R0.1-SNAPSHOT", authors = { "Obadiah Crowe" })
public class Akkadia implements Expansion {

    private AkkadiaConfig config;

    protected AkkadiaLogger logger;
    protected AkkadiaPluginManager pluginManager;
    protected AkkadiaScheduler scheduler;

    @Override
    public void onStartup() {
        this.config = LoadableConfig.getByClass(AkkadiaConfig.class).load();

        this.logger = new AkkadiaLogger();
        this.pluginManager = new AkkadiaPluginManager(this);
        this.scheduler = new AkkadiaScheduler(this);

        Bukkit.setServer(new AkkadiaServer(this)); // Set Bukkit server implementation.

        // No reason to check if exists, as Skyfall creates it before loading Akkadia.
        this.pluginManager.loadPlugins(Server.get().getPath().resolve("expansions").toFile());
    }

    @Override
    public void onShutdown() {}
}
