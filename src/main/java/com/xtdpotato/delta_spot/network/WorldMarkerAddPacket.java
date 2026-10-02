package com.xtdpotato.delta_spot.network;

import com.xtdpotato.delta_spot.DeltaSpot;
import com.xtdpotato.delta_spot.client.WorldMarkerClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server broadcast for a newly created tactical marker. */
public record WorldMarkerAddPacket(UUID markerId, UUID ownerId, String dimension, int kind, boolean enemy,
                                   int teamColor, String itemId,
                                   double x, double y, double z, int remainingTicks)
        implements CustomPacketPayload {
    public static final Type<WorldMarkerAddPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(DeltaSpot.MOD_ID, "world_marker_add"));
    public static final StreamCodec<FriendlyByteBuf, WorldMarkerAddPacket> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> {
            buf.writeUUID(packet.markerId);
            buf.writeUUID(packet.ownerId);
            buf.writeUtf(packet.dimension, 128);
            buf.writeVarInt(packet.kind);
            buf.writeBoolean(packet.enemy);
            buf.writeVarInt(packet.teamColor);
            buf.writeUtf(packet.itemId == null ? "" : packet.itemId, 256);
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
            buf.writeVarInt(packet.remainingTicks);
        }, buf -> new WorldMarkerAddPacket(buf.readUUID(), buf.readUUID(), buf.readUtf(128),
            buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(256), buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readVarInt()));

    @Override
    public Type<WorldMarkerAddPacket> type() {
        return TYPE;
    }

    public static void handle(WorldMarkerAddPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> WorldMarkerClientState.add(packet));
    }
}
