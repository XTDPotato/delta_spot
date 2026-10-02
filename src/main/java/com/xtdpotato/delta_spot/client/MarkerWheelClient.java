package com.xtdpotato.delta_spot.client;

import com.xtdpotato.delta_spot.Config;
import com.xtdpotato.delta_spot.compat.XeroDeltaCompat;
import com.xtdpotato.delta_spot.network.ModNetwork;
import com.xtdpotato.delta_spot.network.WorldMarkerCreatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Hold-to-open tactical communication wheel for world markers. */
public final class MarkerWheelClient {
    private static final double MAX_MARK_DISTANCE = 999.0D;
    private static boolean keyWasDown;
    private static boolean active;
    private static boolean closing;
    private static long animationStartNanos;
    private static long closingStartNanos;
    private static float closingOpacity;
    private static final long OPEN_NANOS = 180_000_000L;
    private static final long CLOSE_NANOS = 160_000_000L;
    private static int holdTicks;
    private static int selected = -1;
    private static float pointerX;
    private static float pointerY;
    private static long lastQuickClickNanos;
    private static Vec3 lastQuickClickPosition;
    private static boolean middleMouseDown;

    private MarkerWheelClient() {
    }

    public static void tick(Minecraft minecraft, boolean keyDown) {
        keyDown = keyDown || middleMouseDown;
        if (minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()
            || XeroDeltaCompat.interactionLocked()) {
            reset(minecraft);
            return;
        }
        if (!keyDown) {
            if (keyWasDown) completeRelease(minecraft);
            if (closing && System.nanoTime() - closingStartNanos < CLOSE_NANOS) return;
            reset(minecraft);
            return;
        }
        if (!keyWasDown) {
            begin(minecraft);
        }
        keyWasDown = true;
        WheelMouseController.keepHidden(minecraft);
        holdTicks++;
        if (!active && holdTicks >= holdTicks(XeroDeltaCompat.markerWheelHoldSeconds(
            Config.INSTANCE.markerWheelHoldSeconds.get()))) {
            active = true;
            animationStartNanos = System.nanoTime();
        }
    }

    public static boolean active() {
        return active;
    }

    public static void onMiddleButton(Minecraft minecraft, int action) {
        if (minecraft.player == null || minecraft.screen != null) return;
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS && !keyWasDown) {
            middleMouseDown = true;
            begin(minecraft);
            keyWasDown = true;
        } else if (action == org.lwjgl.glfw.GLFW.GLFW_RELEASE) {
            middleMouseDown = false;
            if (keyWasDown) completeRelease(minecraft);
        }
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        if ((!active && !closing) || minecraft.player == null) return;
        float animation = opacity(System.nanoTime());
        if (animation < 0.01F) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int centerY = Math.max(72, height / 2);
        int radius = Math.min(142, Math.max(82, Math.min(width, height) / 3));
        int innerRadius = Math.max(34, Math.round(radius * 0.43F));
        int configuredSlots = XeroDeltaCompat.markerWheelSlots(
            Config.INSTANCE.markerWheelSlots.get());
        List<MarkerWheelCatalog.Entry> entries = MarkerWheelCatalog.entries(
            configuredSlots == 4 ? 4 : 8);
        if (active) {
            WheelMouseController.keepHidden(minecraft);
            WheelMouseController.confineToCircle(minecraft, centerX, centerY,
                Math.max(innerRadius + 2.0F, radius - 5.0F));
            updateSelection(minecraft, radius, entries.size());
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(0.92F + animation * 0.08F, 0.92F + animation * 0.08F, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);
        int accent = selected >= 0 ? entries.get(selected).color() : 0xFF80908D;
        RadialWheelRenderer.draw(graphics, centerX, centerY, innerRadius, radius,
            entries.size(), selected, accent, animation);
        for (int index = 0; index < entries.size(); index++) {
            double angle = -Math.PI / 2.0D + Math.PI * 2.0D * index / entries.size();
            double itemRadius = (innerRadius + radius) * 0.52D;
            int x = centerX + (int) Math.round(Math.cos(angle) * itemRadius);
            int y = centerY + (int) Math.round(Math.sin(angle) * itemRadius);
            MarkerWheelCatalog.Entry entry = entries.get(index);
            boolean chosen = index == selected;
            MarkerIconRenderer.drawWheelIcon(graphics, x, y - 7, entry.id(), false,
                fade(chosen ? entry.color() : 0xFF9AA7A5, animation));
            graphics.drawCenteredString(minecraft.font, entry.label(), x, y + 8,
                fade(chosen ? 0xFFFFFFFF : 0xFFC1CAC8, animation));
        }
        graphics.drawCenteredString(minecraft.font,
            selected >= 0 ? entries.get(selected).label()
                : Component.translatable("marker_wheel.delta_spot.title"),
            centerX, centerY - 8, fade(0xFFE8EEEC, animation));
        graphics.drawCenteredString(minecraft.font,
            Component.translatable("marker_wheel.delta_spot.release"),
            centerX, centerY + 6, fade(0xFF96A5A2, animation));
        graphics.pose().popPose();
    }

    private static void releaseWheel(Minecraft minecraft) {
        if (selected < 0) return;
        Target target = findTarget(minecraft);
        if (target != null) {
            ModNetwork.sendToServer(new WorldMarkerCreatePacket(selected, false,
                target.type, target.entityId, target.position.x, target.position.y, target.position.z));
        }
    }

    private static void completeRelease(Minecraft minecraft) {
        if (active) {
            releaseWheel(minecraft);
            closingStartNanos = System.nanoTime();
            closingOpacity = opacity(closingStartNanos);
            closing = true;
            active = false;
            keyWasDown = false;
            WheelMouseController.close(minecraft, WheelMouseController.Owner.MARKER);
        } else {
            quickMark(minecraft);
            reset(minecraft);
        }
    }

    private static void quickMark(Minecraft minecraft) {
        Target target = findTarget(minecraft);
        if (target == null) return;
        long now = System.nanoTime();
        long interval = (long) (XeroDeltaCompat.markerDoubleClickSeconds(
            Config.INSTANCE.markerDoubleClickSeconds.get()) * 1_000_000_000.0D);
        boolean rapid = lastQuickClickNanos != 0L && now - lastQuickClickNanos <= interval
            && lastQuickClickPosition != null
            && lastQuickClickPosition.distanceTo(target.position) <= 0.5D;
        boolean enemy = rapid;
        if (rapid) {
            lastQuickClickNanos = 0L;
            lastQuickClickPosition = null;
        } else {
            lastQuickClickNanos = now;
            lastQuickClickPosition = target.position;
        }
        // Kind 8 is reserved for the plain middle-click marker.  Keeping it
        // separate prevents it from being rendered as the wheel's attack icon.
        ModNetwork.sendToServer(new WorldMarkerCreatePacket(enemy ? 0 : 8, enemy,
            target.type, target.entityId, target.position.x, target.position.y, target.position.z));
    }

    private static void begin(Minecraft minecraft) {
        closing = false;
        active = false;
        WheelMouseController.open(minecraft, WheelMouseController.Owner.MARKER);
        holdTicks = 0;
        selected = -1;
        pointerX = 0.0F;
        pointerY = 0.0F;
    }

    private static Target findTarget(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return null;
        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getViewVector(1.0F).scale(MAX_MARK_DISTANCE));
        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(start, end,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
            ? MAX_MARK_DISTANCE * MAX_MARK_DISTANCE
            : start.distanceToSqr(blockHit.getLocation());
        AABB search = minecraft.player.getBoundingBox()
            .expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(minecraft.player, start, end,
            search, entity -> entity != minecraft.player && entity.isPickable(),
            MAX_MARK_DISTANCE * MAX_MARK_DISTANCE);
        if (entityHit != null && start.distanceToSqr(entityHit.getLocation()) <= blockDistance) {
            Entity entity = entityHit.getEntity();
            return new Target(0, entity.getId(), entity.position());
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return new Target(1, -1, blockHit.getLocation());
        }
        return null;
    }

    private static void updateSelection(Minecraft minecraft, int radius, int entries) {
        float centerX = minecraft.getWindow().getGuiScaledWidth() * 0.5F;
        float centerY = Math.max(72.0F, minecraft.getWindow().getGuiScaledHeight() * 0.5F);
        pointerX = WheelMouseController.guiX(minecraft) - centerX;
        pointerY = WheelMouseController.guiY(minecraft) - centerY;
        float distance = (float) Math.sqrt(pointerX * pointerX + pointerY * pointerY);
        if (distance < 18.0F) {
            selected = -1;
            return;
        }
        double sector = Math.PI * 2.0D / entries;
        double normalized = (Math.atan2(pointerY, pointerX) + Math.PI / 2.0D + Math.PI * 2.0D)
            % (Math.PI * 2.0D);
        selected = (int) Math.floor((normalized + sector / 2.0D) / sector) % entries;
    }

    private static int holdTicks(double seconds) {
        return Math.max(1, (int) Math.ceil(Math.max(0.10D, seconds) * 20.0D));
    }

    private static void reset(Minecraft minecraft) {
        WheelMouseController.close(minecraft, WheelMouseController.Owner.MARKER);
        keyWasDown = false;
        active = false;
        closing = false;
        holdTicks = 0;
        selected = -1;
        pointerX = 0.0F;
        pointerY = 0.0F;
        middleMouseDown = false;
    }

    private static float opacity(long now) {
        float progress = Math.max(0.0F, Math.min(1.0F,
            (now - (closing ? closingStartNanos : animationStartNanos))
                / (float) (closing ? CLOSE_NANOS : OPEN_NANOS)));
        return closing ? closingOpacity * (1.0F - progress * progress * progress)
            : 1.0F - (float) Math.pow(1.0F - progress, 3);
    }

    private static int fade(int color, float opacity) {
        // Font treats alpha values below four as fully opaque.
        return Math.max(4, Math.round((color >>> 24) * opacity)) << 24 | color & 0xFFFFFF;
    }

    private record Target(int type, int entityId, Vec3 position) {
    }
}
