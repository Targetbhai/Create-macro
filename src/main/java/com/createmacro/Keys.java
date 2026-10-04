package com.createmacro;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Helpers for the "k:<glfw key>" / "m:<mouse button>" key encoding used in the config. */
public final class Keys {
	private Keys() {}

	public static String keyboard(int glfwKey) {
		return "k:" + glfwKey;
	}

	public static String mouse(int button) {
		return "m:" + button;
	}

	public static InputConstants.Key decode(String enc) {
		int code = Integer.parseInt(enc.substring(2));
		return enc.startsWith("m:")
				? InputConstants.Type.MOUSE.getOrCreate(code)
				: InputConstants.Type.KEYSYM.getOrCreate(code);
	}

	public static String displayName(String enc) {
		try {
			String s = decode(enc).getDisplayName().getString();
			return s.isEmpty() ? enc : s;
		} catch (Exception e) {
			return enc;
		}
	}

	public static String displayNames(List<String> keys) {
		if (keys.isEmpty()) return "(none)";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0) sb.append(" + ");
			sb.append(displayName(keys.get(i)));
		}
		return sb.toString();
	}

	/** True while the physical key / mouse button is held down. Call on the render thread. */
	public static boolean isDown(String enc) {
		long window = GLFW.glfwGetCurrentContext();
		if (window == 0L) return false;
		int code = Integer.parseInt(enc.substring(2));
		if (enc.startsWith("m:")) {
			return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
		}
		return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
	}
}
