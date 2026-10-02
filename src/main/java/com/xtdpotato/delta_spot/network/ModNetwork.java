package com.xtdpotato.delta_spot.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(VERSION);
        registrar.playToServer(WorldMarkerCreatePacket.TYPE,
            WorldMarkerCreatePacket.STREAM_CODEC, WorldMarkerCreatePacket::handle);
        registrar.playToClient(WorldMarkerAddPacket.TYPE,
            WorldMarkerAddPacket.STREAM_CODEC, WorldMarkerAddPacket::handle);
        registrar.playToClient(WorldMarkerRemovePacket.TYPE,
            WorldMarkerRemovePacket.STREAM_CODEC, WorldMarkerRemovePacket::handle);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
