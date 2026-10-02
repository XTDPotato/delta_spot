package com.xtdpotato.delta_spot.client;

import com.xtdpotato.delta_spot.network.WorldMarkerAddPacket;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Client-side marker cache and camera-projected world marker renderer. */
public final class WorldMarkerClientState {
    private static final long POP_DURATION_NANOS = 200_000_000L;
    private static final long FADE_DURATION_NANOS = 500_000_000L;
    private static final Map<UUID, Marker> MARKERS = new LinkedHashMap<>();
    private static Object lastLevel;

    private WorldMarkerClientState() {
    }

    public static void add(WorldMarkerAddPacket packet) {
        long now = System.nanoTime();
        MARKERS.put(packet.markerId(), new Marker(packet.ownerId(), packet.dimension(), packet.kind(),
            packet.enemy(), packet.teamColor(), packet.itemId(),
            new Vec3(packet.x(), packet.y(), packet.z()),
            now, now + Math.max(1L, packet.remainingTicks()) * 50_000_000L));
    }

    public static void remove(UUID markerId) {
        MARKERS.remove(markerId);
    }

    public static void clear() {
        MARKERS.clear();
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return;
        if (lastLevel != null && lastLevel != minecraft.level) {
            MARKERS.clear();
        }
        lastLevel = minecraft.level;
        if (MARKERS.isEmpty()) return;
        String dimension = minecraft.level.dimension().location().toString();
        long now = System.nanoTime();
        MARKERS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (MARKERS.isEmpty()) return;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vector3f lookVector = camera.getLookVector();
        Vector3f upVector = camera.getUpVector();
        Vector3f leftVector = camera.getLeftVector();
        Vec3 forward = new Vec3(lookVector.x, lookVector.y, lookVector.z);
        Vec3 right = new Vec3(-leftVector.x, -leftVector.y, -leftVector.z);
        Vec3 up = new Vec3(upVector.x, upVector.y, upVector.z);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        double fov = Math.max(30.0D, Math.min(120.0D, minecraft.options.fov().get()));
        double focal = height / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        for (Marker marker : MARKERS.values()) {
            if (!dimension.equals(marker.dimension)) continue;
            Vec3 relative = marker.position.subtract(cameraPos);
            double depth = relative.dot(forward);
            double horizontal = relative.dot(right);
            double vertical = relative.dot(up);
            double screenX = width * 0.5D + horizontal / Math.max(0.01D, depth) * focal;
            double screenY = height * 0.5D - vertical / Math.max(0.01D, depth) * focal;
            int safeLeft = 54;
            int safeRight = Math.max(safeLeft, width - 54);
            int safeTop = 22;
            int safeBottom = Math.max(safeTop, height - 42);
            boolean onScreen = depth > 0.05D && screenX >= safeLeft && screenX <= safeRight
                && screenY >= safeTop && screenY <= safeBottom;
            double baseX = screenX;
            double baseY = screenY;
            double directionX = horizontal;
            double directionY = -vertical;
            if (!onScreen) {
                if (depth <= 0.05D) {
                    // A marker passing behind the camera must keep its horizontal
                    // bearing. Mirroring this vector makes a right-side marker
                    // jump to the left edge after the player turns past it.
                    // Backward targets settle at the bottom edge, matching the
                    // direction a player should turn to bring them back on screen.
                    double horizontalMagnitude = Math.abs(directionX);
                    directionY = Math.max(directionY,
                        Math.max(1.0D, horizontalMagnitude * 0.28D));
                }
                if (Math.abs(directionX) + Math.abs(directionY) < 0.0001D) directionY = 1.0D;
                double halfWidth = Math.max(1.0D, (safeRight - safeLeft) * 0.5D);
                double halfHeight = Math.max(1.0D, (safeBottom - safeTop) * 0.5D);
                double scale = Math.min(halfWidth / Math.max(0.0001D, Math.abs(directionX)),
                    halfHeight / Math.max(0.0001D, Math.abs(directionY)));
                screenX = width * 0.5D + directionX * scale;
                screenY = (safeTop + safeBottom) * 0.5D + directionY * scale;
                screenX = Math.max(safeLeft, Math.min(safeRight, screenX));
                screenY = Math.max(safeTop, Math.min(safeBottom, screenY));
            }
            int x = (int) Math.round(screenX);
            int y = (int) Math.round(screenY);
            double appearAlpha = Math.min(1.0D,
                Math.max(0.0D, (now - marker.createdAt) / 100_000_000.0D));
            double fadeAlpha = Math.min(1.0D,
                Math.max(0.0D, (marker.expiresAt - now) / (double) FADE_DURATION_NANOS));
            int alpha = Math.max(0, Math.min(255,
                (int) Math.round(255.0D * appearAlpha * fadeAlpha)));
            int baseColor = marker.enemy ? 0xFFE6372D : marker.teamColor;
            int color = alpha << 24 | (baseColor & 0x00FFFFFF);
            int distance = (int) Math.round(minecraft.player.position().distanceTo(marker.position));
            double markerScale = distanceScale(distance) * popScale(now - marker.createdAt);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 460.0F);
            if (onScreen) {
                Vec3 topRelative = marker.position.add(0.0D, 2.0D, 0.0D).subtract(cameraPos);
                double topDepth = topRelative.dot(forward);
                int topX = x;
                int topY = y - 28;
                if (topDepth > 0.05D) {
                    topX = (int) Math.round(width * 0.5D + topRelative.dot(right) / topDepth * focal);
                    topY = (int) Math.round(height * 0.5D - topRelative.dot(up) / topDepth * focal);
                }
                // The ground anchor and its guide line are only useful nearby;
                // at range keep the marker itself uncluttered.
                if (distance <= 5) {
                    drawLine(graphics, (int) Math.round(baseX), (int) Math.round(baseY), topX, topY, color);
                    drawMarkerCircle(graphics, (int) Math.round(baseX),
                        (int) Math.round(baseY), 5, color);
                }
                graphics.drawCenteredString(minecraft.font,
                    net.minecraft.network.chat.Component.translatable(
                        "marker_wheel.delta_spot.distance", distance),
                    topX, marker.itemId.isBlank() ? topY - 15 : topY - 9,
                    alpha << 24 | 0xFFFFFF);
                if (marker.itemId.isBlank()) {
                    if (marker.kind == 8) MarkerIconRenderer.drawPlainMarker(graphics, topX, topY,
                        (int) Math.round(12.0D * markerScale), color, false);
                    else MarkerIconRenderer.drawWorldIcon(graphics, topX, topY,
                        (int) Math.round(12.0D * markerScale), marker.kind, marker.enemy, color);
                } else renderItemIcon(graphics, marker.itemId, topX,
                    topY + 10, alpha, markerScale);
            } else {
                renderOffscreenMarker(graphics, minecraft, marker, x, y,
                    directionX, directionY, distance, color, alpha, markerScale);
            }
            graphics.pose().popPose();
        }
    }

    private static void renderOffscreenMarker(GuiGraphics graphics, Minecraft minecraft,
                                              Marker marker, int anchorX, int anchorY,
                                              double directionX, double directionY,
                                              int distance, int color, int alpha,
                                              double markerScale) {
        double length = Math.sqrt(directionX * directionX + directionY * directionY);
        if (length < 0.0001D) {
            directionX = 0.0D;
            directionY = 1.0D;
        } else {
            directionX /= length;
            directionY /= length;
        }
        var distanceText = net.minecraft.network.chat.Component.translatable(
            "marker_wheel.delta_spot.distance", distance);
        int distanceWidth = minecraft.font.width(distanceText);
        int groupWidth = 18 + 26 + 5 + distanceWidth;
        int left = Math.max(4, Math.min(
            minecraft.getWindow().getGuiScaledWidth() - groupWidth - 4,
            anchorX - groupWidth / 2));
        int centerY = Math.max(14, Math.min(
            minecraft.getWindow().getGuiScaledHeight() - 14, anchorY));
        int arrowX = left + 7;
        int iconX = left + 29;
        drawDirectionArrow(graphics, minecraft, arrowX, centerY,
            directionX, directionY, color);
        if (marker.itemId.isBlank()) {
            if (marker.kind == 8) MarkerIconRenderer.drawPlainMarker(graphics, iconX, centerY,
                (int) Math.round(12.0D * markerScale), color, true);
            else MarkerIconRenderer.drawWorldIcon(graphics, iconX, centerY,
                (int) Math.round(12.0D * markerScale), marker.kind, marker.enemy, color);
        } else renderItemIcon(graphics, marker.itemId, iconX, centerY, alpha, markerScale);
        graphics.drawString(minecraft.font, distanceText, iconX + 16,
            centerY - minecraft.font.lineHeight / 2, alpha << 24 | 0xFFFFFF, true);
    }

    private static void renderItemIcon(GuiGraphics graphics, String itemId,
                                       int centerX, int centerY, int alpha,
                                       double scale) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return;
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) return;
        ItemStack stack = new ItemStack(item);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.pose().translate(-8.0F, -8.0F, 0.0F);
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().popPose();
    }

    private static double distanceScale(int distance) {
        return distance <= 5 ? 0.75D : 0.5D;
    }

    private static double popScale(long ageNanos) {
        double progress = Math.min(1.0D,
            Math.max(0.0D, ageNanos / (double) POP_DURATION_NANOS));
        double shifted = progress - 1.0D;
        return 1.0D + 2.70158D * shifted * shifted * shifted
            + 1.70158D * shifted * shifted;
    }

    private static void drawDirectionArrow(GuiGraphics graphics, Minecraft minecraft,
                                           int centerX, int centerY,
                                           double directionX, double directionY,
                                           int color) {
        String glyph = ">";
        float angle = (float) Math.atan2(directionY, directionX);
        int glyphWidth = minecraft.font.width(glyph);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotation(angle));
        graphics.drawString(minecraft.font, glyph, -glyphWidth / 2,
            -minecraft.font.lineHeight / 2, color, true);
        graphics.pose().popPose();
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0,
                                 int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int doubled = error * 2;
            if (doubled >= dy) {
                error += dy;
                x0 += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }

    private static void drawMarkerCircle(GuiGraphics graphics, int centerX,
                                         int centerY, int radius, int color) {
        int previousX = centerX + radius;
        int previousY = centerY;
        for (int step = 1; step <= 24; step++) {
            double angle = Math.PI * 2.0D * step / 24.0D;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            drawLine(graphics, previousX, previousY, x, y, color);
            previousX = x;
            previousY = y;
        }
    }

    private record Marker(UUID ownerId, String dimension, int kind, boolean enemy, int teamColor,
                          String itemId,
                          Vec3 position, long createdAt, long expiresAt) {
    }
}
