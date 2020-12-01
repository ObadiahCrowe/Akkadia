package io.skyfallsdk.akkadia;

import io.skyfallsdk.akkadia.logger.LoggerWrapper;
import io.skyfallsdk.akkadia.plugin.AkkadiaPluginManager;
import io.skyfallsdk.akkadia.scheduler.AkkadiaScheduler;
import io.skyfallsdk.expansion.Expansion;
import io.skyfallsdk.expansion.ExpansionInfo;
import org.bukkit.Bukkit;

@ExpansionInfo(name = "Akkadia", version = "${bukkit.version}", authors = { "Obadiah Crowe" })
public class Akkadia implements Expansion {

    protected LoggerWrapper loggerWrapper;
    protected AkkadiaPluginManager pluginManager;
    protected AkkadiaScheduler scheduler;

    @Override
    public void onStartup() {
        this.loggerWrapper = new LoggerWrapper();
        this.pluginManager = new AkkadiaPluginManager(this);
        this.scheduler = new AkkadiaScheduler(this);

        Bukkit.setServer(new AkkadiaServer(this)); // Set Bukkit server implementation.
    }

    @Override
    public void onShutdown() {}
}
