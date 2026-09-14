package com.redgear.reverie.client;

import com.redgear.reverie.ReverieDestinationPayload;
import com.redgear.reverie.ReverieDestinationSelectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class ReverieDestinationScreen extends Screen {
    private static final int ROWS = 6;
    private final List<ReverieDestinationPayload.Destination> destinations;
    private int page;

    public ReverieDestinationScreen(List<ReverieDestinationPayload.Destination> destinations) {
        super(Component.translatable("screen.reverie.destinations"));
        this.destinations = destinations;
    }

    @Override protected void init() {
        clearWidgets();
        int left = width / 2 - 110;
        int top = height / 2 - 76;
        int start = page * ROWS;
        for (int row = 0; row < ROWS && start + row < destinations.size(); row++) {
            var destination = destinations.get(start + row);
            Component name = destination.entryBed() ? Component.translatable("screen.reverie.entry_bed")
                    : Component.literal(destination.name());
            Component label = destination.available() ? name
                    : Component.translatable("screen.reverie.destination_unavailable", name);
            Button button = Button.builder(label, ignored -> select(destination))
                    .bounds(left, top + row * 22, 220, 20).build();
            button.active = destination.available();
            addRenderableWidget(button);
        }
        int pages = Math.max(1, (destinations.size() + ROWS - 1) / ROWS);
        if (pages > 1) {
            Button previous = Button.builder(Component.literal("<"), ignored -> { page--; rebuildWidgets(); })
                    .bounds(left, top + 134, 32, 20).build();
            previous.active = page > 0;
            addRenderableWidget(previous);
            Button next = Button.builder(Component.literal(">"), ignored -> { page++; rebuildWidgets(); })
                    .bounds(left + 188, top + 134, 32, 20).build();
            next.active = page + 1 < pages;
            addRenderableWidget(next);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
                .bounds(left + 55, top + 160, 110, 20).build());
    }

    private void select(ReverieDestinationPayload.Destination destination) {
        PacketDistributor.sendToServer(new ReverieDestinationSelectPayload(
                destination.name(), destination.entryBed(), destination.savedBed()));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 100, 0xFFFFFF);
    }

    @Override public boolean isPauseScreen() { return false; }
}
