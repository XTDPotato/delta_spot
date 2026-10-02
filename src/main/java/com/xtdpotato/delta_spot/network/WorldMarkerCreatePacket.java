package com.xtdpotato.delta_spot.network;

import com.xtdpotato.delta_spot.DeltaSpot;
import com.xtdpotato.delta_spot.data.WorldMarkerServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client request to place a server-authoritative tactical marker. */
public record WorldMarkerCreatePacket(int kind, boolean enemy, int targetType, int entityId,
                                      double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<WorldMarkerCreatePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(DeltaSpot.MOD_ID, "world_marker_create"));
    public static final StreamCodec<FriendlyByteBuf, WorldMarkerCreatePacket> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> {
            buf.writeVarInt(packet.kind);
            buf.writeBoolean(packet.enemy);
            buf.writeByte(packet.targetType);
            buf.writeVarInt(packet.entityId);
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
        }, buf -> new WorldMarkerCreatePacket(buf.readVarInt(), buf.readBoolean(), buf.readByte(),
            buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<WorldMarkerCreatePacket> type() {
        return TYPE;
    }

    public static void handle(WorldMarkerCreatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                WorldMarkerServerState.create(player, packet.kind(), packet.enemy(),
                    packet.targetType(), packet.entityId(), new Vec3(packet.x(), packet.y(), packet.z()));
            }
        });
    }
}
