package com.xtdpotato.delta_spot.client;

import net.minecraft.client.gui.GuiGraphics;

/** Shared tactical marker icon vocabulary for the wheel and marker HUD. */
public final class MarkerIconRenderer {
    private MarkerIconRenderer() {
    }

    public static void drawWheelIcon(GuiGraphics graphics, int x, int y,
                                     int kind, boolean enemy, int color) {
        draw(graphics, x, y, 11, kind, enemy, color, false);
    }

    public static void drawWorldIcon(GuiGraphics graphics, int x, int y,
                                     int kind, boolean enemy, int color) {
        draw(graphics, x, y, 12, kind, enemy, color, true);
    }

    public static void drawWorldIcon(GuiGraphics graphics, int x, int y, int radius,
                                     int kind, boolean enemy, int color) {
        draw(graphics, x, y, Math.max(3, radius), kind, enemy, color, true);
    }

    public static void drawPlainMarker(GuiGraphics graphics, int x, int y,
                                       int radius, int color, boolean darkBacking) {
        int size = Math.max(3, radius);
        if (darkBacking) drawFilledDiamond(graphics, x, y, size, 0x80000000);
        drawDiamondOutline(graphics, x, y, size, color);
        drawFilledDiamond(graphics, x, y, Math.max(1, Math.round(size * 0.26F)), color);
    }

    private static void draw(GuiGraphics graphics, int x, int y, int radius,
                             int kind, boolean enemy, int color, boolean darkBacking) {
        if (darkBacking) drawFilledDiamond(graphics, x, y, radius, 0x80000000);
        drawDiamondOutline(graphics, x, y, radius, color);

        if (enemy || kind == 0) {
            drawTarget(graphics, x, y, radius, color);
        } else if (kind == 1) {
            drawAttack(graphics, x, y, radius, color);
        } else if (kind == 6) {
            drawShield(graphics, x, y, radius, color);
        } else if (kind == 7) {
            drawEye(graphics, x, y, radius, color);
        } else {
            drawTeammate(graphics, x, y, radius, color);
        }
    }

    private static void drawTarget(GuiGraphics graphics, int x, int y, int radius, int color) {
        int inner = Math.max(3, radius / 3);
        drawCircle(graphics, x, y, inner, color);
        int armStart = inner + 2;
        int armEnd = Math.max(armStart + 1, radius - 3);
        graphics.fill(x - 1, y - armEnd, x + 2, y - armStart, color);
        graphics.fill(x - 1, y + armStart, x + 2, y + armEnd + 1, color);
        graphics.fill(x - armEnd, y - 1, x - armStart, y + 2, color);
        graphics.fill(x + armStart, y - 1, x + armEnd + 1, y + 2, color);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
    }

    private static void drawAttack(GuiGraphics graphics, int x, int y, int radius, int color) {
        int inset = Math.max(3, radius / 3);
        drawLine(graphics, x - radius + inset, y + radius - inset, x - 1, y - radius + inset, color);
        drawLine(graphics, x - 1, y - radius + inset, x + radius - inset, y + radius - inset, color);
        drawLine(graphics, x - inset, y + radius - inset, x + radius - inset, y - 1, color);
    }

    private static void drawShield(GuiGraphics graphics, int x, int y, int radius, int color) {
        int top = y - radius / 2;
        int left = x - radius / 3;
        int right = x + radius / 3;
        int bottom = y + radius / 2;
        drawLine(graphics, left, top, right, top, color);
        drawLine(graphics, left, top, left, y + 2, color);
        drawLine(graphics, right, top, right, y + 2, color);
        drawLine(graphics, left, y + 2, x, bottom, color);
        drawLine(graphics, right, y + 2, x, bottom, color);
    }

    private static void drawEye(GuiGraphics graphics, int x, int y, int radius, int color) {
        int horizontal = Math.max(4, radius / 2);
        int vertical = Math.max(2, radius / 4);
        drawLine(graphics, x - horizontal, y, x - 1, y - vertical, color);
        drawLine(graphics, x - 1, y - vertical, x + horizontal, y, color);
        drawLine(graphics, x + horizontal, y, x - 1, y + vertical, color);
        drawLine(graphics, x - 1, y + vertical, x - horizontal, y, color);
        drawCircle(graphics, x, y, Math.max(2, vertical), color);
    }

    private static void drawTeammate(GuiGraphics graphics, int x, int y, int radius, int color) {
        int headRadius = Math.max(2, radius / 5);
        drawFilledCircle(graphics, x, y - headRadius - 1, headRadius, color);
        int shoulder = Math.max(4, radius / 2);
        int top = y + headRadius + 1;
        graphics.fill(x - shoulder, top + 2, x + shoulder + 1, top + 4, color);
        graphics.fill(x - shoulder + 1, top, x + shoulder, top + 3, color);
        graphics.fill(x - shoulder + 2, top - 1, x + shoulder - 1, top + 2, color);
    }

    private static void drawDiamondOutline(GuiGraphics graphics, int x, int y, int radius, int color) {
        drawLine(graphics, x, y - radius, x + radius, y, color);
        drawLine(graphics, x + radius, y, x, y + radius, color);
        drawLine(graphics, x, y + radius, x - radius, y, color);
        drawLine(graphics, x - radius, y, x, y - radius, color);
    }

    private static void drawFilledDiamond(GuiGraphics graphics, int x, int y, int radius, int color) {
        for (int row = -radius; row <= radius; row++) {
            int half = radius - Math.abs(row);
            graphics.fill(x - half, y + row, x + half + 1, y + row + 1, color);
        }
    }

    private static void drawFilledCircle(GuiGraphics graphics, int x, int y, int radius, int color) {
        for (int row = -radius; row <= radius; row++) {
            int half = (int) Math.sqrt(radius * radius - row * row);
            graphics.fill(x - half, y + row, x + half + 1, y + row + 1, color);
        }
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
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

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
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
}
