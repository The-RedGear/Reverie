package com.redgear.reverie;

import com.redgear.reverie.client.ReverieClient;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ReverieNetwork {
    private ReverieNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(ReverieBedOccupancyPayload.TYPE,
                ReverieBedOccupancyPayload.STREAM_CODEC, ReverieClient::handleBedOccupancy);
    }
}
