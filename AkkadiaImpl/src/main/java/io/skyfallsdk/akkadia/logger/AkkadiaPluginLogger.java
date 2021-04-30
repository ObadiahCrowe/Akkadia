package io.skyfallsdk.akkadia.logger;

import io.skyfallsdk.Server;
import org.apache.logging.log4j.Level;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;

import java.util.logging.LogRecord;

public class AkkadiaPluginLogger extends PluginLogger {

    /**
     * Creates a new PluginLogger that extracts the name from a plugin.
     *
     * @param context A reference to the plugin
     */
    public AkkadiaPluginLogger(Plugin context) {
        super(context);
    }

    @Override
    public void log(LogRecord record) {
        Server.get().getLogger().log(Level.getLevel(record.getLevel().getName()), record.getMessage(), record.getParameters());
    }
}
