package com.redgear.reverie;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class ReverieNetwork {
    private ReverieNetwork() {}
    public static Consumer<ReverieDestinationPayload> CLIENT_DESTINATIONS = payload -> {};
    public static LongConsumer CLIENT_LIGHTING = time -> {};

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(ReverieDestinationPayload.TYPE, ReverieDestinationPayload.STREAM_CODEC,
                (payload, context) -> CLIENT_DESTINATIONS.accept(payload));
        registrar.playToClient(ReverieLightingPayload.TYPE, ReverieLightingPayload.STREAM_CODEC,
                (payload, context) -> CLIENT_LIGHTING.accept(payload.time()));
        registrar.playToServer(ReverieDestinationSelectPayload.TYPE, ReverieDestinationSelectPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player)
                        ReverieEvents.useCompassDestination(player, payload.name(), payload.entryBed(), payload.savedBed());
                });
    }
}
