package com.createmacro;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Macro editor:
 *  - name
 *  - trigger key
 *  - "only while holding" item
 *  - steps: keys (several allowed) + delay slider 0-100 ms, with an "Add more" button
 */
public class MacroEditScreen extends Screen {
	private static final int ROW_H = 26;
	private static final int FORM_W = 338;

	private final Screen parent;
	private final Macro macro;

	private final List<Row> rows = new ArrayList<>();
	private int scroll = 0;

	private Button triggerBtn;
	private Button holdBtn;

	/** Non-null while waiting for the next key / mouse button. */
	private Consumer<String> recorder = null;
	/** Which button is currently "listening" (used to show "Press a key..."). */
	private Object recordingTag = null;

	public MacroEditScreen(Screen parent, Macro macro) {
		super(Component.literal("Edit macro"));
		this.parent = parent;
		this.macro = macro;
	}

	private int rowsTop() {
		return 92;
	}

	private int rowsBottom() {
		return this.height - 34;
	}

	// ---------------------------------------------------------------- widgets

	private class Row {
		final Macro.Step step;
		Button keysBtn, clearBtn, removeBtn;
		DelaySlider slider;

		Row(Macro.Step step) {
			this.step = step;
		}

		List<AbstractWidget> widgets() {
			return List.of(keysBtn, clearBtn, slider, removeBtn);
		}

		void refreshLabel() {
			if (recordingTag == this) {
				keysBtn.setMessage(Component.literal("Press a key / click...").withStyle(ChatFormatting.YELLOW));
			} else {
				keysBtn.setMessage(Component.literal(Keys.displayNames(step.keys)));
			}
		}
	}

	private static class DelaySlider extends AbstractSliderButton {
		private final Macro.Step step;

		DelaySlider(int x, int y, int w, int h, Macro.Step step) {
			super(x, y, w, h, Component.empty(), step.delay / 100.0);
			this.step = step;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal("Delay: " + Math.round(this.value * 100) + " ms"));
		}

		@Override
		protected void applyValue() {
			step.delay = (int) Math.round(this.value * 100);
		}
	}

	@Override
	protected void init() {
		rows.clear();
		int cx = this.width / 2;
		int x0 = cx - FORM_W / 2;

		// --- name
		EditBox nameBox = new EditBox(this.font, cx - 100, 16, 200, 18, Component.literal("Macro name"));
		nameBox.setMaxLength(32);
		nameBox.setValue(macro.name);
		nameBox.setResponder(s -> macro.name = s.isEmpty() ? "Macro" : s);
		this.addRenderableWidget(nameBox);

		// --- trigger key
		triggerBtn = Button.builder(Component.empty(), b -> {
			startRecording(triggerBtn, key -> macro.trigger = key);
		}).bounds(x0, 40, FORM_W / 2 - 2, 20).build();
		this.addRenderableWidget(triggerBtn);

		// --- holding preset (left click = next, shift/right via second press cycles too)
		holdBtn = Button.builder(Component.empty(), b -> {
			macro.holdingPreset = (macro.holdingPreset + 1) % ItemMatcher.PRESETS.length;
			macro.customItems = "";
			this.rebuildWidgets();
		}).bounds(x0 + FORM_W / 2 + 2, 40, FORM_W / 2 - 2, 20).build();
		this.addRenderableWidget(holdBtn);

		// --- custom items
		EditBox customBox = new EditBox(this.font, x0, 64, FORM_W, 18, Component.literal("Custom items"));
		customBox.setMaxLength(120);
		customBox.setHint(Component.literal("Custom items (optional): totem, trident, minecraft:bow ...")
				.withStyle(ChatFormatting.DARK_GRAY));
		customBox.setValue(macro.customItems == null ? "" : macro.customItems);
		customBox.setResponder(s -> macro.customItems = s);
		this.addRenderableWidget(customBox);

		// --- step rows
		for (Macro.Step step : macro.steps) {
			Row row = new Row(step);
			row.keysBtn = Button.builder(Component.empty(), b -> startRecording(row, key -> {
				if (!step.keys.contains(key)) step.keys.add(key);
			})).bounds(x0, 0, 150, 20).build();
			row.clearBtn = Button.builder(Component.literal("Clear"), b -> {
				step.keys.clear();
				cancelRecording();
				refreshLabels();
			}).bounds(x0 + 154, 0, 42, 20).build();
			row.slider = new DelaySlider(x0 + 200, 0, 112, 20, step);
			row.removeBtn = Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> {
				if (macro.steps.size() > 1) {
					macro.steps.remove(step);
					cancelRecording();
					this.rebuildWidgets();
				}
			}).bounds(x0 + 316, 0, 22, 20).build();

			for (AbstractWidget w : row.widgets()) this.addRenderableWidget(w);
			rows.add(row);
		}

		// --- bottom buttons
		this.addRenderableWidget(Button.builder(Component.literal("+ Add more"), b -> {
			macro.steps.add(new Macro.Step());
			cancelRecording();
			scroll = Integer.MAX_VALUE; // jump to the new step
			this.rebuildWidgets();
		}).bounds(x0, this.height - 28, FORM_W / 2 - 2, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(x0 + FORM_W / 2 + 2, this.height - 28, FORM_W / 2 - 2, 20).build());

		clampScroll();
		refreshLabels();
	}

	private void refreshLabels() {
		if (recordingTag == triggerBtn) {
			triggerBtn.setMessage(Component.literal("Press a key / click...").withStyle(ChatFormatting.YELLOW));
		} else {
			String t = macro.trigger == null ? "click to set" : Keys.displayName(macro.trigger);
			triggerBtn.setMessage(Component.literal("Trigger key: " + t));
		}
		holdBtn.setMessage(Component.literal("Holding: " + ItemMatcher.presetName(macro.holdingPreset)));
		for (Row r : rows) r.refreshLabel();
	}

	// ---------------------------------------------------------------- recording keys

	private void startRecording(Object tag, Consumer<String> target) {
		this.setFocused(null);
		recordingTag = tag;
		recorder = target;
		refreshLabels();
	}

	private void cancelRecording() {
		recorder = null;
		recordingTag = null;
		refreshLabels();
	}

	private void finishRecording(String encodedKey) {
		if (recorder != null) recorder.accept(encodedKey);
		cancelRecording();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (recorder != null) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				cancelRecording();
			} else {
				finishRecording(Keys.keyboard(event.key()));
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (recorder != null) {
			finishRecording(Keys.mouse(event.button()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	// ---------------------------------------------------------------- scrolling / layout

	private void clampScroll() {
		int viewport = rowsBottom() - rowsTop();
		int max = Math.max(0, rows.size() * ROW_H - viewport);
		scroll = Math.max(0, Math.min(max, scroll));
	}

	private void layoutRows() {
		for (int i = 0; i < rows.size(); i++) {
			int y = rowsTop() + i * ROW_H - scroll;
			boolean visible = y >= rowsTop() && y + 20 <= rowsBottom();
			for (AbstractWidget w : rows.get(i).widgets()) {
				w.setY(y);
				w.visible = visible;
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) (scrollY * 20);
		clampScroll();
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		layoutRows();
		super.render(g, mouseX, mouseY, partialTick);

		int x0 = this.width / 2 - FORM_W / 2;
		g.drawString(this.font, "Name:", Math.max(2, this.width / 2 - 100 - 34), 21, 0xFFFFFFFF);

		for (int i = 0; i < rows.size(); i++) {
			int y = rowsTop() + i * ROW_H - scroll;
			if (y >= rowsTop() && y + 20 <= rowsBottom()) {
				g.drawString(this.font, (i + 1) + ".", Math.max(2, x0 - 14), y + 6, 0xFFAAAAAA);
			}
		}

		if (recorder != null) {
			String msg = "Press a key or mouse button (Esc to cancel). Click the key button again to add more keys.";
			g.drawString(this.font, msg, this.width / 2 - this.font.width(msg) / 2, rowsTop() - 8, 0xFFFFFF55);
		}
	}

	@Override
	public void onClose() {
		MacroConfig.save();
		this.minecraft.setScreen(parent);
	}
}
