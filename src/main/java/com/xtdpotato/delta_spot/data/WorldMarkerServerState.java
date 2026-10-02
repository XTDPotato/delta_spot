package com.xtdpotato.delta_spot.data;

import com.xtdpotato.delta_spot.Config;
import com.xtdpotato.delta_spot.DeltaSpot;
import com.xtdpotato.delta_spot.client.TeamColorPalette;
import com.xtdpotato.delta_spot.compat.XeroDeltaCompat;
import com.xtdpotato.delta_spot.network.ModNetwork;
import com.xtdpotato.delta_spot.network.WorldMarkerAddPacket;
import com.xtdpotato.delta_spot.network.WorldMarkerRemovePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-authoritative lifetime and replacement policy for tactical markers. */
@EventBusSubscriber(modid = DeltaSpot.MOD_ID)
public final class WorldMarkerServerState {
    private static final double MAX_DISTANCE_SQUARED = 999.0D * 999.0D;
    private static final Map<MinecraftServer, Map<UUID, Marker>> STATES = new WeakHashMap<>();

    private WorldMarkerServerState() {
    }

    public static void create(ServerPlayer owner, int kind, boolean enemy, int targetType,
                              int entityId, Vec3 position) {
        if (owner == null || kind < 0 || kind > 8 || position == null
            || owner.position().distanceToSqr(position) > MAX_DISTANCE_SQUARED
            || (targetType != 0 && targetType != 1)) return;
        if (targetType == 0) {
            var target = owner.level().getEntity(entityId);
            if (target == null || !target.isPickable()
                || target.distanceToSqr(owner) > MAX_DISTANCE_SQUARED
                || target.position().distanceToSqr(position) > 16.0D
                || !entityIsOnViewRay(owner, target)) return;
        } else {
            Vec3 start = owner.getEyePosition();
            Vec3 direction = position.subtract(start);
            if (direction.lengthSqr() < 0.0001D) return;
            Vec3 verificationEnd = position.add(direction.normalize().scale(0.75D));
            var clip = owner.level().clip(new net.minecraft.world.level.ClipContext(
                start, verificationEnd,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
            if (clip.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || clip.getLocation().distanceToSqr(position) > 1.0D) return;
        }
        MinecraftServer server = owner.server;
        Map<UUID, Marker> markers = STATES.computeIfAbsent(server, ignored -> new java.util.LinkedHashMap<>());
        markers.values().stream()
            .filter(marker -> marker.ownerId.equals(owner.getUUID()))
            .map(marker -> marker.id)
            .toList()
            .forEach(id -> remove(server, markers, id));

        double configuredLifetime = enemy
            ? XeroDeltaCompat.enemyMarkerLifetimeSeconds(
                Config.INSTANCE.enemyMarkerLifetimeSeconds.get())
            : XeroDeltaCompat.markerLifetimeSeconds(
                Config.INSTANCE.markerLifetimeSeconds.get());
        long lifetimeMillis = Math.round(Math.max(1.0D,
            Math.min(60.0D, configuredLifetime)) * 1000.0D);
        String itemId = "";
        if (targetType == 0 && owner.level().getEntity(entityId) instanceof ItemEntity itemEntity) {
            var itemKey = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
            itemId = itemKey == null ? "" : itemKey.toString();
        }
        Marker marker = new Marker(UUID.randomUUID(), owner.getUUID(),
            owner.level().dimension().location().toString(), kind, enemy, itemId, position,
            teamColor(owner), System.currentTimeMillis() + lifetimeMillis, System.nanoTime());
        markers.put(marker.id, marker);
        broadcast(server, marker.ownerId, new WorldMarkerAddPacket(marker.id, marker.ownerId,
            marker.dimension, marker.kind, marker.enemy,
            marker.teamColor, marker.itemId,
            marker.position.x, marker.position.y, marker.position.z,
            Math.max(1, (int) Math.ceil(lifetimeMillis / 50.0D))));
    }

    private static boolean entityIsOnViewRay(ServerPlayer owner,
                                              net.minecraft.world.entity.Entity target) {
        Vec3 start = owner.getEyePosition();
        Vec3 end = start.add(owner.getViewVector(1.0F).scale(999.0D));
        var block = owner.level().clip(new ClipContext(start, end,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, owner));
        double maxDistance = block.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? MAX_DISTANCE_SQUARED : start.distanceToSqr(block.getLocation());
        AABB search = owner.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(owner, start, end, search,
            entity -> entity != owner && entity.isPickable(), MAX_DISTANCE_SQUARED);
        return hit != null && hit.getEntity() == target
            && start.distanceToSqr(hit.getLocation()) <= maxDistance + 0.01D;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Map<UUID, Marker> markers = STATES.get(server);
        if (markers == null || markers.isEmpty()) return;
        long now = System.currentTimeMillis();
        new ArrayList<>(markers.values()).stream()
            .filter(marker -> marker.expiresAt <= now)
            .map(marker -> marker.id)
            .forEach(id -> remove(server, markers, id));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Map<UUID, Marker> markers = STATES.get(player.server);
        if (markers == null) return;
        long now = System.currentTimeMillis();
        markers.values().stream()
            .filter(marker -> marker.expiresAt > now)
            .filter(marker -> FtbTeamIntegration.memberIdsIncludingSelf(player)
                .contains(marker.ownerId))
            .forEach(marker -> ModNetwork.sendToClient(player, new WorldMarkerAddPacket(
                marker.id, marker.ownerId, marker.dimension, marker.kind, marker.enemy,
                marker.teamColor, marker.itemId,
                marker.position.x, marker.position.y, marker.position.z,
                Math.max(1, (int) Math.ceil((marker.expiresAt - now) / 50.0D)))));
    }

    private static int teamColor(ServerPlayer owner) {
        var ids = new java.util.ArrayList<>(FtbTeamIntegration.memberIdsIncludingSelf(owner));
        int index = Math.max(0, ids.indexOf(owner.getUUID()));
        return TeamColorPalette.colorForNumber(index + 1);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        STATES.remove(event.getServer());
    }

    private static void remove(MinecraftServer server, Map<UUID, Marker> markers, UUID id) {
        Marker marker = markers.remove(id);
        if (marker != null) broadcast(server, marker.ownerId, new WorldMarkerRemovePacket(id));
    }

    private static void broadcast(MinecraftServer server, UUID ownerId, Object packet) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        Set<UUID> recipients = owner == null ? Set.of(ownerId)
            : FtbTeamIntegration.memberIdsIncludingSelf(owner);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!recipients.contains(player.getUUID())) continue;
            if (packet instanceof WorldMarkerAddPacket add) ModNetwork.sendToClient(player, add);
            else if (packet instanceof WorldMarkerRemovePacket remove) ModNetwork.sendToClient(player, remove);
        }
    }

    private static final class Marker {
        private final UUID id;
        private final UUID ownerId;
        private final String dimension;
        private final int kind;
        private final boolean enemy;
        private final String itemId;
        private final Vec3 position;
        private final int teamColor;
        private final long expiresAt;
        private final long createdAt;

        private Marker(UUID id, UUID ownerId, String dimension, int kind, boolean enemy, String itemId, Vec3 position,
                       int teamColor, long expiresAt, long createdAt) {
            this.id = id;
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.kind = kind;
            this.enemy = enemy;
            this.itemId = itemId == null ? "" : itemId;
            this.position = position;
            this.teamColor = teamColor;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }
    }
}
