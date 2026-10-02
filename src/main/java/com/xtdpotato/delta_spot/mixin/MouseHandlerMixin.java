package com.xtdpotato.delta_spot.mixin;

import com.xtdpotato.delta_spot.client.WheelMouseController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Spot's hidden marker-wheel pointer inside its radial input area. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onMove", at = @At("TAIL"))
    private void deltaSpot$constrainWheelPointer(long window, double x, double y,
                                                  CallbackInfo callback) {
        WheelMouseController.constrainOnMouseMove(Minecraft.getInstance());
    }
}
