package com.modstaff.tool.feature;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * A small GUI that just wraps the normal chat commands your server's
 * moderation plugin already provides (e.g. EssentialsX / LuckPerms-style
 * /ban, /mute, /tp, /vanish). It does not bypass server-side permissions -
 * if the account running the client is not staff on the server, these
 * commands will simply be rejected by the server like any other command.
 * Adjust the command strings below to match your own server's plugin.
 */
public class ModGuiScreen extends Screen {

    private TextFieldWidget targetField;
    private TextFieldWidget reasonField;

    public ModGuiScreen() {
        super(Text.translatable("modstafftool.gui.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 60;

        targetField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20,
                Text.translatable("modstafftool.gui.target"));
        targetField.setMaxLength(32);
        addDrawableChild(targetField);
        setInitialFocus(targetField);

        y += 26;
        reasonField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20,
                Text.literal("Reason (optional)"));
        reasonField.setMaxLength(128);
        addDrawableChild(reasonField);

        y += 30;
        addDrawableChild(ButtonWidget.builder(Text.translatable("modstafftool.gui.tp"), b -> runCommand(
                "tp " + target() + " " + selfName())).dimensions(centerX - 100, y, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("modstafftool.gui.vanish"), b -> runCommand(
                "vanish")).dimensions(centerX + 5, y, 95, 20).build());

        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.translatable("modstafftool.gui.mute"), b -> runCommand(
                "mute " + target() + " " + reason())).dimensions(centerX - 100, y, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("modstafftool.gui.ban"), b -> runCommand(
                "ban " + target() + " " + reason())).dimensions(centerX + 5, y, 95, 20).build());
    }

    private String target() {
        String t = targetField.getText().trim();
        return t.isEmpty() ? "" : t;
    }

    private String reason() {
        String r = reasonField.getText().trim();
        return r.isEmpty() ? "Rule violation" : r;
    }

    private String selfName() {
        return client != null && client.player != null ? client.player.getGameProfile().getName() : "";
    }

    private void runCommand(String command) {
        if (client == null || client.player == null || target().isEmpty()) return;
        client.player.networkHandler.sendChatCommand(command.trim());
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 90, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
