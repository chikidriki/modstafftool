package com.modstaff.tool.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * Draws a simple wireframe box through terrain for every other player in
 * render distance - a staff aid for spotting where players are (e.g. someone
 * hiding to grief, or locating a player who filed a report).
 */
public class EspRenderer {

    public static boolean enabled = false;

    public static void onRenderWorldLast(WorldRenderContext context) {
        if (!enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferAllocator allocator = new BufferAllocator(1536);
        var bufferBuilder = new net.minecraft.client.render.BufferBuilder(
                allocator, net.minecraft.client.render.VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (PlayerEntity otherPlayer : client.world.getPlayers()) {
            if (otherPlayer == client.player) continue;

            double interpX = MathHelper.lerp(context.tickCounter().getTickProgress(true),
                    otherPlayer.lastRenderX, otherPlayer.getX());
            double interpY = MathHelper.lerp(context.tickCounter().getTickProgress(true),
                    otherPlayer.lastRenderY, otherPlayer.getY());
            double interpZ = MathHelper.lerp(context.tickCounter().getTickProgress(true),
                    otherPlayer.lastRenderZ, otherPlayer.getZ());

            Box box = otherPlayer.getBoundingBox()
                    .offset(interpX - otherPlayer.getX(), interpY - otherPlayer.getY(), interpZ - otherPlayer.getZ())
                    .offset(-camX, -camY, -camZ)
                    .expand(0.05);

            float r = 1.0f, g = 0.25f, b = 0.25f, a = 0.85f;
            drawBoxOutline(bufferBuilder, matrix, box, r, g, b, a);
        }

        matrices.pop();

        var built = bufferBuilder.end();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawBoxOutline(net.minecraft.client.render.BufferBuilder buf, Matrix4f matrix,
                                        Box box, float r, float g, float b, float a) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // 12 edges of the box, each as a line (two vertices).
        float[][] edges = {
                {x1,y1,z1, x2,y1,z1}, {x2,y1,z1, x2,y1,z2}, {x2,y1,z2, x1,y1,z2}, {x1,y1,z2, x1,y1,z1},
                {x1,y2,z1, x2,y2,z1}, {x2,y2,z1, x2,y2,z2}, {x2,y2,z2, x1,y2,z2}, {x1,y2,z2, x1,y2,z1},
                {x1,y1,z1, x1,y2,z1}, {x2,y1,z1, x2,y2,z1}, {x2,y1,z2, x2,y2,z2}, {x1,y1,z2, x1,y2,z2},
        };
        for (float[] e : edges) {
            buf.vertex(matrix, e[0], e[1], e[2]).color(r, g, b, a);
            buf.vertex(matrix, e[3], e[4], e[5]).color(r, g, b, a);
        }
    }
}
