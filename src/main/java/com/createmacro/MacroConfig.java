package com.createmacro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads and saves all macros to config/createmacro.json. */
public final class MacroConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static List<Macro> macros = new ArrayList<>();

	private MacroConfig() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("createmacro.json");
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try {
			String json = Files.readString(f);
			List<Macro> loaded = GSON.fromJson(json, new TypeToken<List<Macro>>() {}.getType());
			if (loaded != null) macros = loaded;
		} catch (Exception e) {
			System.err.println("[Create Macro] Failed to load config: " + e);
		}
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(macros));
		} catch (IOException e) {
			System.err.println("[Create Macro] Failed to save config: " + e);
		}
	}
}
