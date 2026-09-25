package com.modstaff.tool.feature;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Polls block state in a radius around the local player once per second and
 * logs any change (break/place) to a local file, tagging the nearest other
 * player as a best-guess "likely actor".
 *
 * Important caveat: this is a heuristic, not ground truth. A vanilla client
 * has no reliable way to attribute a block change to a specific player
 * without server-side support (e.g. CoreProtect/logging plugin + a command
 * that exposes results, or your own server plugin broadcasting a custom
 * packet). For real evidence in punishments, pair this with a server-side
 * logging plugin (CoreProtect, Prism, etc.) - use this client log only as a
 * quick "something changed near player X, go check /co lookup" pointer.
 */
public class BlockLogger {

    public static boolean enabled = true;
    private static final int RADIUS = 12;
    private static final int POLL_INTERVAL_TICKS = 20;

    private final Map<BlockPos, BlockState> lastKnown = new HashMap<>();
    private int tickCounter = 0;
    private Path logFile;

    public void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < POLL_INTERVAL_TICKS) return;
        tickCounter = 0;

        BlockPos center = client.player.getBlockPos();
        Map<BlockPos, BlockState> current = new HashMap<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = client.world.getBlockState(pos);
                    current.put(pos, state);

                    BlockState previous = lastKnown.get(pos);
                    if (previous != null && previous != state
                            && !(previous.isOf(Blocks.AIR) && state.isOf(Blocks.AIR))) {
                        logChange(client, pos, previous, state);
                    }
                }
            }
        }

        lastKnown.clear();
        lastKnown.putAll(current);
    }

    private void logChange(MinecraftClient client, BlockPos pos, BlockState before, BlockState after) {
        PlayerEntity nearest = findNearestOtherPlayer(client, pos);
        String actor = nearest != null ? nearest.getGameProfile().getName() : "unknown";
        boolean broken = after.isOf(Blocks.AIR) && !before.isOf(Blocks.AIR);

        String line = String.format("[%s] %s %s at %d,%d,%d (likely: %s)",
                LocalDateTime.now(),
                broken ? "BREAK" : "PLACE/CHANGE",
                broken ? before.getBlock().getName().getString() : after.getBlock().getName().getString(),
                pos.getX(), pos.getY(), pos.getZ(),
                actor);

        writeLine(line);
    }

    private PlayerEntity findNearestOtherPlayer(MinecraftClient client, BlockPos pos) {
        PlayerEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player) continue;
            double d = p.getBlockPos().getSquaredDistance(pos.getX(), pos.getY(), pos.getZ());
            if (d < bestDist) {
                bestDist = d;
                nearest = p;
            }
        }
        // Only trust the guess if the closest player is within ~6 blocks of the change.
        return (nearest != null && bestDist <= 36) ? nearest : null;
    }

    private void writeLine(String line) {
        try {
            if (logFile == null) {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("modstafftool-logs");
                dir.toFile().mkdirs();
                logFile = dir.resolve("block-changes.log");
            }
            try (FileWriter writer = new FileWriter(logFile.toFile(), true)) {
                writer.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            // Swallow: logging must never crash the client.
        }
    }
}
