package com.xtdpotato.delta_spot;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final Config INSTANCE;
    public static final ModConfigSpec SPEC;

    public final ModConfigSpec.ConfigValue<Integer> markerWheelSlots;
    public final ModConfigSpec.DoubleValue markerWheelHoldSeconds;
    public final ModConfigSpec.DoubleValue markerDoubleClickSeconds;
    public final ModConfigSpec.DoubleValue markerLifetimeSeconds;
    public final ModConfigSpec.DoubleValue enemyMarkerLifetimeSeconds;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new Config(builder);
        SPEC = builder.build();
    }

    private Config(ModConfigSpec.Builder builder) {
        builder.push("marker");
        markerWheelSlots = builder.comment("Number of radial marker slots: 4 or 8; values below 6 use four slots")
            .defineInRange("wheelSlots", 8, 4, 8);
        markerWheelHoldSeconds = builder.comment("Seconds to hold before opening the wheel")
            .defineInRange("wheelHoldSeconds", 0.75D, 0.10D, 3.0D);
        markerDoubleClickSeconds = builder.comment("Maximum interval for an enemy-marker double click")
            .defineInRange("doubleClickSeconds", 1.0D, 0.10D, 3.0D);
        markerLifetimeSeconds = builder.comment("Normal marker lifetime")
            .defineInRange("lifetimeSeconds", 20.0D, 1.0D, 300.0D);
        enemyMarkerLifetimeSeconds = builder.comment("Enemy marker lifetime")
            .defineInRange("enemyLifetimeSeconds", 10.0D, 1.0D, 300.0D);
        builder.pop();
    }
}
