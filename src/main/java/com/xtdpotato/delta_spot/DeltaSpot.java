package com.xtdpotato.delta_spot;

import com.mojang.logging.LogUtils;
import com.xtdpotato.delta_spot.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(DeltaSpot.MOD_ID)
public final class DeltaSpot {
    public static final String MOD_ID = "delta_spot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeltaSpot(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::register);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
