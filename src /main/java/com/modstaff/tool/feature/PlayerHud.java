package com.modstaff.tool.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

/**
 * Draws a small panel in the top-left corner listing nearby players with
 * distance and ping, so a moderator can quickly see who is around without
 * opening the tab list.
 */
public class PlayerHud {

    public static boolean enabled = true;
    private static final double RANGE = 64.0;

    public static void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        List<PlayerEntity> nearby = client.world.getPlayers().stream()
                .filter(p -> p != client.player)
                .filter(p -> p.squaredDistanceTo(client.player) <= RANGE * RANGE)
                .sorted(Comparator.comparingDouble(p -> p.squaredDistanceTo(client.player)))
                .limit(12)
                .toList();

        int x = 6;
        int y = 6;
        int lineHeight = 10;

        context.drawTextWithShadow(client.textRenderer, Text.literal("Nearby players (" + nearby.size() + ")"),
                x, y, 0xFFFFFF00);
        y += lineHeight + 2;

        for (PlayerEntity p : nearby) {
            double dist = Math.sqrt(p.squaredDistanceTo(client.player));
            int ping = 0;
            PlayerListEntry entry = client.getNetworkHandler() != null
                    ? client.getNetworkHandler().getPlayerListEntry(p.getUuid())
                    : null;
            if (entry != null) {
                ping = entry.getLatency();
            }

            String line = String.format("%s  %.0fm  %dms", p.getGameProfile().getName(), dist, ping);
            int color = ping > 300 ? 0xFFFF5555 : 0xFFAAAAAA;
            context.drawTextWithShadow(client.textRenderer, Text.literal(line), x, y, color);
            y += lineHeight;
        }
    }
}
