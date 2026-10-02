package com.xtdpotato.delta_spot.compat;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/** Optional reflection bridge. Delta Spot never requires Xero Delta at link time. */
public final class XeroDeltaCompat {
    private XeroDeltaCompat() {
    }

    public static boolean interactionLocked() {
        try {
            Class<?> stateClass = Class.forName(
                "com.xtdpotato.xero_delta.client.DownedClientState");
            Field instanceField = stateClass.getField("INSTANCE");
            Object state = instanceField.get(null);
            return (boolean) stateClass.getMethod("interactionLocked").invoke(state);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<UUID> memberIdsIncludingSelf(ServerPlayer player) {
        try {
            Class<?> integration = Class.forName(
                "com.xtdpotato.xero_delta.data.FtbTeamIntegration");
            Method method = integration.getMethod("memberIdsIncludingSelf", ServerPlayer.class);
            Object value = method.invoke(null, player);
            return value instanceof Set<?> set ? (Set<UUID>) set : Set.of();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Set.of();
        }
    }

    public static int teamColor(int number) {
        try {
            Class<?> palette = Class.forName(
                "com.xtdpotato.xero_delta.client.TeamColorPalette");
            return (int) palette.getMethod("colorForNumber", int.class).invoke(null, number);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return 0;
        }
    }

    public static int markerWheelSlots(int fallback) {
        return readConfigNumber("markerWheelSlots", fallback).intValue();
    }

    public static double markerWheelHoldSeconds(double fallback) {
        return readConfigNumber("markerWheelHoldSeconds", fallback).doubleValue();
    }

    public static double markerDoubleClickSeconds(double fallback) {
        return readConfigNumber("markerDoubleClickSeconds", fallback).doubleValue();
    }

    public static double markerLifetimeSeconds(double fallback) {
        return readConfigNumber("markerLifetimeSeconds", fallback).doubleValue();
    }

    public static double enemyMarkerLifetimeSeconds(double fallback) {
        return readConfigNumber("enemyMarkerLifetimeSeconds", fallback).doubleValue();
    }

    private static Number readConfigNumber(String fieldName, Number fallback) {
        try {
            Class<?> configClass = Class.forName("com.xtdpotato.xero_delta.Config");
            Object config = configClass.getField("INSTANCE").get(null);
            Object configValue = configClass.getField(fieldName).get(config);
            Object value = configValue.getClass().getMethod("get").invoke(configValue);
            return value instanceof Number number ? number : fallback;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return fallback;
        }
    }
}
