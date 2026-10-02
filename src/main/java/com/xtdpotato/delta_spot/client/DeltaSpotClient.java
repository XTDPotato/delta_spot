package com.xtdpotato.delta_spot.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xtdpotato.delta_spot.DeltaSpot;
import com.xtdpotato.delta_spot.compat.XeroDeltaCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = DeltaSpot.MOD_ID, dist = Dist.CLIENT)
public final class DeltaSpotClient {
    public static final KeyMapping MARKER_WHEEL_KEY = new KeyMapping(
        "key.delta_spot.marker_wheel", InputConstants.Type.MOUSE,
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "key.categories.delta_spot");

    public DeltaSpotClient(ModContainer modContainer) {
        IEventBus modBus = modContainer.getEventBus();
        modBus.addListener(this::registerKeys);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(DeltaSpotClient.class);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MARKER_WHEEL_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        MarkerWheelClient.tick(minecraft, MARKER_WHEEL_KEY.isDown());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
            || minecraft.player == null || minecraft.screen != null
            || XeroDeltaCompat.interactionLocked()) return;
        MarkerWheelClient.onMiddleButton(minecraft, event.getAction());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) return;
        RenderSystem.disableScissor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        event.getGuiGraphics().pose().pushPose();
        try {
            WorldMarkerClientState.render(event.getGuiGraphics(), minecraft);
            MarkerWheelClient.render(event.getGuiGraphics(), minecraft);
        } finally {
            event.getGuiGraphics().pose().popPose();
            RenderSystem.disableScissor();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }
}
