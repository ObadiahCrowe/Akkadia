package io.skyfallsdk.akkadia.logger;

import io.skyfallsdk.Server;
import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AkkadiaLogger extends Logger {

    static final Map<java.util.logging.Level, Level> LEVELS_TO_LEVELS = new HashMap<>() {{
        put(java.util.logging.Level.ALL, Level.ALL);
        put(java.util.logging.Level.WARNING, Level.WARN);
        put(java.util.logging.Level.SEVERE, Level.FATAL);
        put(java.util.logging.Level.INFO, Level.INFO);
        put(java.util.logging.Level.CONFIG, Level.DEBUG);
        put(java.util.logging.Level.FINE, Level.INFO);
        put(java.util.logging.Level.FINER, Level.INFO);
        put(java.util.logging.Level.FINEST, Level.INFO);
    }};

    public AkkadiaLogger() {
        super("Akkadia", null);

        this.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Server.get().getLogger().log(LEVELS_TO_LEVELS.getOrDefault(record.getLevel(), Level.INFO), record.getMessage(), record.getParameters());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
    }
}
