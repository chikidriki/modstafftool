package com.modstaff.tool;

import com.modstaff.tool.feature.BlockLogger;
import com.modstaff.tool.feature.ContextMenuScreen;
import com.modstaff.tool.feature.EspRenderer;
import com.modstaff.tool.feature.FreecamManager;
import com.modstaff.tool.feature.ModGuiScreen;
import com.modstaff.tool.feature.PlayerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entry point. This mod has environment=client in fabric.mod.json,
 * so none of this ever loads on a dedicated server - it only affects the
 * player running it (intended: server staff).
 */
public class ModStaffToolClient implements ClientModInitializer {

    public static final String MOD_ID = "modstafftool";

    public static KeyBinding freecamKey;
    public static KeyBinding espKey;
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modstafftool.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "key.category.modstafftool"
        ));
        espKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modstafftool.esp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.category.modstafftool"
        ));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modstafftool.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.category.modstafftool"
        ));

        FreecamManager freecam = new FreecamManager();
        BlockLogger blockLogger = new BlockLogger();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (freecamKey.wasPressed()) {
                freecam.toggle();
            }
            while (espKey.wasPressed()) {
                EspRenderer.enabled = !EspRenderer.enabled;
                sendActionBarToggleMessage(client, "Staff ESP", EspRenderer.enabled);
            }
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ModGuiScreen());
                }
            }

            freecam.tick(client);
            blockLogger.tick(client);
        });

        WorldRenderEvents.LAST.register(EspRenderer::onRenderWorldLast);
        HudRenderCallback.EVENT.register(PlayerHud::onHudRender);

        // Right-click a player while in spectator mode -> open the staff
        // context menu instead of the normal (usually no-op) interaction.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || !client.player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PlayerEntity target) || target == client.player) {
                return ActionResult.PASS;
            }
            if (client.currentScreen == null) {
                double scale = client.getWindow().getScaleFactor();
                int mouseX = (int) (client.mouse.getX() / scale);
                int mouseY = (int) (client.mouse.getY() / scale);
                client.setScreen(new ContextMenuScreen(target.getGameProfile().getName(), mouseX, mouseY));
            }
            return ActionResult.SUCCESS;
        });
    }

    private static void sendActionBarToggleMessage(MinecraftClient client, String name, boolean on) {
        if (client.player != null) {
            client.player.sendMessage(
                    net.minecraft.text.Text.literal("[ModStaffTool] " + name + ": " + (on ? "ON" : "OFF")),
                    true
            );
        }
    }
}
