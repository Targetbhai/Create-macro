package com.createmacro;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Watches trigger keys every tick and plays macro steps on a worker thread with real-time delays. */
public final class MacroRunner {
	private MacroRunner() {}

	private static final Map<Macro, Boolean> wasDown = new IdentityHashMap<>();
	private static final Set<Macro> running = ConcurrentHashMap.newKeySet();

	/** Called once per client tick. */
	public static void poll(Minecraft mc) {
		boolean canFire = mc.player != null && mc.screen == null;

		for (Macro macro : new ArrayList<>(MacroConfig.macros)) {
			if (macro.trigger == null || !macro.enabled) {
				wasDown.remove(macro);
				continue;
			}
			boolean down;
			try {
				down = Keys.isDown(macro.trigger);
			} catch (Exception e) {
				down = false;
			}
			boolean before = wasDown.getOrDefault(macro, false);
			wasDown.put(macro, down);

			if (down && !before && canFire && !running.contains(macro) && ItemMatcher.matches(macro, mc)) {
				start(mc, macro);
			}
		}
	}

	private static void start(Minecraft mc, Macro macro) {
		// Snapshot so editing in the GUI can't break a run in progress.
		List<List<InputConstants.Key>> keys = new ArrayList<>();
		List<Integer> delays = new ArrayList<>();
		for (Macro.Step step : macro.steps) {
			List<InputConstants.Key> k = new ArrayList<>();
			for (String enc : step.keys) {
				try {
					k.add(Keys.decode(enc));
				} catch (Exception ignored) {
				}
			}
			keys.add(k);
			delays.add(Math.max(0, Math.min(100, step.delay)));
		}

		running.add(macro);
		Thread t = new Thread(() -> {
			try {
				for (int i = 0; i < keys.size(); i++) {
					List<InputConstants.Key> k = keys.get(i);
					mc.execute(() -> press(mc, k));
					int d = delays.get(i);
					if (d > 0) Thread.sleep(d);
					mc.execute(() -> release(k));
				}
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			} finally {
				running.remove(macro);
			}
		}, "CreateMacro-" + macro.name);
		t.setDaemon(true);
		t.start();
	}

	private static void press(Minecraft mc, List<InputConstants.Key> keys) {
		if (mc.player == null || mc.screen != null) return;
		for (InputConstants.Key key : keys) {
			KeyMapping.click(key);
			KeyMapping.set(key, true);
		}
	}

	private static void release(List<InputConstants.Key> keys) {
		for (InputConstants.Key key : keys) {
			KeyMapping.set(key, false);
		}
	}
}
