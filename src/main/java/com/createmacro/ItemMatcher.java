package com.createmacro;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** "Only run while holding ..." conditions. Matching is done on the item id, so it also works for new items. */
public final class ItemMatcher {
	private ItemMatcher() {}

	public static final String[] PRESETS = {
			"Any item",
			"Sword",
			"Ender pearl",
			"End crystal",
			"Obsidian",
			"Respawn anchor",
			"Glowstone",
			"Mace",
			"Elytra",
			"Spear",
			"Axe",
			"Totem of undying",
			"Golden apple",
			"Shield",
			"Wind charge",
			"Firework rocket",
			"Cobweb",
			"Water bucket"
	};

	public static String presetName(int index) {
		if (index < 0 || index >= PRESETS.length) index = 0;
		return PRESETS[index];
	}

	private static boolean presetMatches(int preset, String path) {
		return switch (preset) {
			case 0 -> true;
			case 1 -> path.endsWith("_sword");
			case 2 -> path.equals("ender_pearl");
			case 3 -> path.equals("end_crystal");
			case 4 -> path.equals("obsidian");
			case 5 -> path.equals("respawn_anchor");
			case 6 -> path.equals("glowstone");
			case 7 -> path.equals("mace");
			case 8 -> path.equals("elytra");
			case 9 -> path.endsWith("_spear");
			case 10 -> path.endsWith("_axe");
			case 11 -> path.equals("totem_of_undying");
			case 12 -> path.equals("golden_apple") || path.equals("enchanted_golden_apple");
			case 13 -> path.equals("shield");
			case 14 -> path.equals("wind_charge");
			case 15 -> path.equals("firework_rocket");
			case 16 -> path.equals("cobweb");
			case 17 -> path.equals("water_bucket");
			default -> true;
		};
	}

	public static boolean matches(Macro macro, Minecraft mc) {
		if (mc.player == null) return false;
		ItemStack held = mc.player.getMainHandItem();
		String full = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
		String path = full.substring(full.indexOf(':') + 1);

		String custom = macro.customItems == null ? "" : macro.customItems.trim();
		if (!custom.isEmpty()) {
			for (String token : custom.split(",")) {
				String t = token.trim().toLowerCase(Locale.ROOT);
				if (t.isEmpty()) continue;
				if (full.equals(t) || path.equals(t) || path.contains(t)) return true;
			}
			return false;
		}
		return presetMatches(macro.holdingPreset, path);
	}
}
