package io.skyfallsdk.akkadia.logger;

import io.skyfallsdk.Server;
import org.apache.logging.log4j.Level;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerWrapper extends Logger {

    public LoggerWrapper() {
        super("Akkadia", null);

        this.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Server.get().getLogger().log(Level.getLevel(record.getLevel().getName()), record.getMessage(), record.getParameters());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
    }
}
