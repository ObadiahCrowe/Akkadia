package io.skyfallsdk.akkadia;

import io.skyfallsdk.expansion.Expansion;
import io.skyfallsdk.expansion.ExpansionInfo;
import org.bukkit.Bukkit;

@ExpansionInfo(name = "Akkadia", version = "${bukkit.version}", authors = { "Obadiah Crowe" })
public class Akkadia implements Expansion {

    @Override
    public void onStartup() {
        Bukkit.setServer(new AkkadiaServer(this)); // Set Bukkit server implementation.
    }

    @Override
    public void onShutdown() {

    }
}
