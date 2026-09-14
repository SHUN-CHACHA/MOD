package dev.shuncha.headfirework;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import org.lwjgl.glfw.GLFW;

public class HeadFireworkModClient implements ClientModInitializer {

    private static KeyMapping openConfigKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(HeadFireworkMod.MOD_ID, "main"));

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.headfirework.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (!client.hasControlDown() || client.player == null) {
                    continue;
                }
                // OP権限(サーバー側の /headfirework config と同じ GAMEMASTERS 相当)が無ければ、画面自体を開かせない
                if (client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    client.setScreenAndShow(new HeadFireworkConfigScreen());
                } else {
                    client.player.sendSystemMessage(
                            Component.literal("この設定画面を開くにはOP権限が必要です。"));
                }
            }
        });
    }
}
