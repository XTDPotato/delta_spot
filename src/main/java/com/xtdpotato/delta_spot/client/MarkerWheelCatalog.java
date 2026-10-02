package com.xtdpotato.delta_spot.client;

import net.minecraft.network.chat.Component;

import java.util.List;

/** Fixed tactical communication markers used by the quick marker wheel. */
public final class MarkerWheelCatalog {
    public record Entry(int id, String translationKey, int color) {
        public Component label() {
            return Component.translatable(translationKey);
        }
    }

    private static final List<Entry> ENTRIES = List.of(
        new Entry(0, "marker_wheel.delta_spot.enemy", 0xFFE86A63),
        new Entry(1, "marker_wheel.delta_spot.attack", 0xFFFF8B62),
        new Entry(2, "marker_wheel.delta_spot.support_2", 0xFF8CC8FF),
        new Entry(3, "marker_wheel.delta_spot.support_3", 0xFFD5A7FF),
        new Entry(4, "marker_wheel.delta_spot.support_4", 0xFFB8C7C9),
        new Entry(5, "marker_wheel.delta_spot.support_1", 0xFF7DE0D0),
        new Entry(6, "marker_wheel.delta_spot.defend", 0xFF68D4AE),
        new Entry(7, "marker_wheel.delta_spot.attention", 0xFFFFD36A)
    );

    private MarkerWheelCatalog() {
    }

    public static List<Entry> entries(int slots) {
        return ENTRIES.subList(0, slots == 4 ? 4 : 8);
    }

    public static Entry entry(int id) {
        return ENTRIES.get(Math.max(0, Math.min(ENTRIES.size() - 1, id)));
    }
}
