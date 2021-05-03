package net.minecraft.server.v1_16_R3;

import io.skyfallsdk.Server;
import io.skyfallsdk.server.ServerState;

public class MinecraftServer {

    private static MinecraftServer instance;

    private volatile boolean isRunning;

    public MinecraftServer() {
        isRunning = Server.get().getState() == ServerState.RUNNING;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public static MinecraftServer getServer() {
        if (instance == null) {
            instance = new MinecraftServer();
        }

        return instance;
    }
}
