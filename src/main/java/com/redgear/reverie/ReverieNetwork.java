package com.redgear.reverie;

import com.redgear.reverie.client.ReverieClient;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ReverieNetwork {
    private ReverieNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ReverieBiomeTintPayload.TYPE,
                ReverieBiomeTintPayload.STREAM_CODEC, ReverieClient::handleBiomeTint);
    }
}
