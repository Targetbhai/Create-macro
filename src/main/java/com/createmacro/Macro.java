package com.createmacro;

import java.util.ArrayList;
import java.util.List;

/** One macro: a trigger key, an optional held-item condition and a list of steps. */
public class Macro {
	public String name = "Macro";
	public boolean enabled = true;
	/** Encoded key, e.g. "k:70" (keyboard) or "m:1" (mouse). null = no trigger set. */
	public String trigger = null;
	/** Index into ItemMatcher.PRESETS. 0 = any item. */
	public int holdingPreset = 0;
	/** Optional comma-separated custom items; overrides the preset when not empty. */
	public String customItems = "";
	public List<Step> steps = new ArrayList<>();

	public Macro() {
		steps.add(new Step());
	}

	public static class Step {
		/** Encoded keys pressed together in this step. */
		public List<String> keys = new ArrayList<>();
		/** Delay in milliseconds (0-100) the keys are held / waited before the next step. */
		public int delay = 20;
	}
}
