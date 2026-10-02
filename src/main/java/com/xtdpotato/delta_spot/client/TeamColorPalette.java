package com.xtdpotato.delta_spot.client;

import com.xtdpotato.delta_spot.compat.XeroDeltaCompat;

/** Stable squad colors, matching Xero Delta when it is installed. */
public final class TeamColorPalette {
    private static final int[] COLORS = {
        0xFF68D4AE, 0xFF6BA8FF, 0xFFFFD36A, 0xFFD5A7FF,
        0xFFFF9F68, 0xFF7DE0D0, 0xFFC1CAC8, 0xFFB4E36D
    };

    private TeamColorPalette() {
    }

    public static int colorForNumber(int number) {
        int xeroColor = XeroDeltaCompat.teamColor(number);
        if (xeroColor != 0) return xeroColor;
        return COLORS[(Math.max(1, number) - 1) % COLORS.length];
    }
}
