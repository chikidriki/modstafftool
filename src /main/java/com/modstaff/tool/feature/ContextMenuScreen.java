package com.modstaff.tool.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Small right-click context menu, similar to the staff-client popups on
 * anarchy servers (Fantime and friends): right-click a player in spectator
 * mode -> a menu with quick actions appears at the cursor instead of the
 * normal interaction.
 *
 * Like {@link ModGuiScreen}, every button here just sends a plain chat
 * command - the server (LuckPerms/EssentialsX/your own plugin) still decides
 * whether the command is allowed. Adjust the command strings to match your
 * server's actual command syntax.
 */
public class ContextMenuScreen extends Screen {

    private final String targetName;
    private final int menuX;
    private final int menuY;

    public ContextMenuScreen(String targetName, int mouseX, int mouseY) {
        super(Text.literal(targetName));
        this.targetName = targetName;
        this.menuX = mouseX;
        this.menuY = mouseY;
    }

    @Override
    protected void init() {
        int width = 110;
        int height = 20;
        int x = Math.min(menuX, this.width - width - 4);
        int y = Math.min(menuY, this.height - 1);

        int i = 0;
        i = addOption(x, y, width, height, i, "Teleport to");
        i = addOption(x, y, width, height, i, "Teleport here");
        i = addOption(x, y, width, height, i, "Freeze");
        i = addOption(x, y, width, height, i, "Mute");
        i = addOption(x, y, width, height, i, "Kick");
        i = addOption(x, y, width, height, i, "Ban");
        i = addOption(x, y, width, height, i, "Inventory");
        addOption(x, y, width, height, i, "Copy name");
    }

    private int addOption(int x, int y, int width, int height, int index, String label) {
        java.util.function.Function<String, String> commandBuilder = switch (label) {
            case "Teleport to" -> t -> "tp " + selfName() + " " + t;
            case "Teleport here" -> t -> "tp " + t + " " + selfName();
            case "Freeze" -> t -> "freeze " + t;
            case "Mute" -> t -> "mute " + t + " 1h Rule violation";
            case "Kick" -> t -> "kick " + t + " Rule violation";
            case "Ban" -> t -> "ban " + t + " Rule violation";
            case "Inventory" -> t -> "invsee " + t;
            default -> t -> null;
        };

        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
            if (label.equals("Copy name")) {
                client.keyboard.setClipboard(targetName);
                this.close();
                return;
            }
            String command = commandBuilder.apply(targetName);
            if (command != null && client != null && client.player != null) {
                client.player.networkHandler.sendChatCommand(command);
            }
            this.close();
        }).dimensions(x, y + index * (height + 2), width, height).build());

        return index + 1;
    }

    private String selfName() {
        return client != null && client.player != null ? client.player.getGameProfile().getName() : "";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No dark background - keep the world visible, this is a small popup.
        context.fill(menuX - 2, menuY - 12, menuX + 112, menuY - 1, 0xAA000000);
        context.drawText(this.textRenderer, targetName, menuX, menuY - 10, 0xFFFFFF00, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty: don't dim the world behind the menu.
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
