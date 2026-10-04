package com.createmacro;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class CreateMacroMod implements ClientModInitializer {
	public static final String MOD_ID = "createmacro";

	private static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		MacroConfig.load();

		// Default: M opens the macro GUI (rebindable in Options > Controls).
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.createmacro.open",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(CreateMacroMod::onTick);
	}

	private static void onTick(Minecraft mc) {
		while (openGuiKey.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new MacroListScreen(null));
			}
		}
		MacroRunner.poll(mc);
	}
}
