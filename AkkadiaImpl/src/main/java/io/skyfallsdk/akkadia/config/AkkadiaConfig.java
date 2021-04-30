package io.skyfallsdk.akkadia.config;

import io.skyfallsdk.Server;
import io.skyfallsdk.akkadia.Akkadia;
import io.skyfallsdk.config.type.YamlConfig;

import java.nio.file.Path;

public class AkkadiaConfig extends YamlConfig<AkkadiaConfig> {

    private static final AkkadiaConfig DEFAULT_CONFIG = new AkkadiaConfig(
      false,
      new CompatibilityHackSection(false)
    );

    private boolean skipLegacyConversion;
    private CompatibilityHackSection compatibility;

    public AkkadiaConfig() {
        super(AkkadiaConfig.class);
    }

    public AkkadiaConfig(boolean skipLegacyConversion, CompatibilityHackSection compatibility) {
        super(AkkadiaConfig.class);

        this.skipLegacyConversion = skipLegacyConversion;
        this.compatibility = compatibility;
    }

    public boolean isSkippingLegacyConversion() {
        return this.skipLegacyConversion;
    }

    public CompatibilityHackSection getCompatibility() {
        return this.compatibility;
    }

    @Override
    public Path getPath() {
        return Server.get().getExpansion(Akkadia.class).getPath().resolve("config.yml");
    }

    @Override
    public AkkadiaConfig getDefaultConfig() {
        return DEFAULT_CONFIG;
    }

    public static class CompatibilityHackSection {

        private boolean enableEssentialsCompat;

        public CompatibilityHackSection() {}

        public CompatibilityHackSection(boolean enableEssentialsCompat) {
            this.enableEssentialsCompat = enableEssentialsCompat;
        }

        public boolean usingEssentialsCompat() {
            return this.enableEssentialsCompat;
        }
    }
}
