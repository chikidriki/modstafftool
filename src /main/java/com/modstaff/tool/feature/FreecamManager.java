package com.modstaff.tool.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * A lightweight freecam: instead of spawning a fake camera entity (which
 * needs mixins into rendering/input to feel smooth), this decouples the
 * render camera from the player entity by directly moving a virtual
 * position/rotation and feeding it to the game renderer's camera each frame
 * via client.gameRenderer.getCamera().
 *
 * NOTE: This skeleton moves the camera but still lets the real player entity
 * exist at its last position (frozen) - movement keys while freecam is on
 * are consumed by this class instead of being sent to the server, so the
 * player does not actually walk anywhere on the server. For production use,
 * test carefully: some servers' anti-cheat may flag camera-only clients
 * differently, and true "detached" freecam usually needs a mixin into
 * ClientPlayerEntity's tick/movement and Camera#update. Treat this as a
 * starting point to refine, not a finished product.
 */
public class FreecamManager {

    private boolean active = false;
    private Vec3d camPos = Vec3d.ZERO;
    private float camYaw;
    private float camPitch;
    private Entity previousCameraEntity;

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        active = !active;
        if (active) {
            previousCameraEntity = client.getCameraEntity();
            camPos = client.player.getPos().add(0, client.player.getEyeHeight(client.player.getPose()), 0);
            camYaw = client.player.getYaw();
            camPitch = client.player.getPitch();
            // Freeze the real player's movement input while freecam is active.
            client.player.setVelocity(Vec3d.ZERO);
        } else {
            client.setCameraEntity(previousCameraEntity != null ? previousCameraEntity : client.player);
        }
    }

    public void tick(MinecraftClient client) {
        if (!active || client.player == null) return;

        // Re-assert that the render camera stays detached from the player
        // entity every tick (some vanilla code resets it on respawn/dimension
        // change etc). Actual smooth free-fly movement (WASD + speed keys)
        // should be wired up in a mixin that intercepts the player's
        // movement input while `active` is true and applies it to camPos
        // instead of the player entity. This class exposes camPos/camYaw/
        // camPitch as the values to feed into that mixin.

        // Keep the real player from drifting due to gravity/knockback.
        client.player.setVelocity(Vec3d.ZERO);
        client.player.fallDistance = 0;
    }

    public Vec3d getCamPos() {
        return camPos;
    }

    public void setCamPos(Vec3d pos) {
        this.camPos = pos;
    }

    public float getCamYaw() {
        return camYaw;
    }

    public float getCamPitch() {
        return camPitch;
    }
}
