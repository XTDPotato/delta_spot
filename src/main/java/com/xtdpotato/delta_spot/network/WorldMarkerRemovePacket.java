package com.xtdpotato.delta_spot.network;

import com.xtdpotato.delta_spot.DeltaSpot;
import com.xtdpotato.delta_spot.client.WorldMarkerClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server broadcast removing an expired or replaced marker. */
public record WorldMarkerRemovePacket(UUID markerId) implements CustomPacketPayload {
    public static final Type<WorldMarkerRemovePacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(DeltaSpot.MOD_ID, "world_marker_remove"));
    public static final StreamCodec<FriendlyByteBuf, WorldMarkerRemovePacket> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> buf.writeUUID(packet.markerId),
            buf -> new WorldMarkerRemovePacket(buf.readUUID()));

    @Override
    public Type<WorldMarkerRemovePacket> type() {
        return TYPE;
    }

    public static void handle(WorldMarkerRemovePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> WorldMarkerClientState.remove(packet.markerId()));
    }
}
