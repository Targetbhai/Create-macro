package com.createmacro;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Main GUI (opened with M): all macros, each with an edit button, ON/OFF toggle and delete button. */
public class MacroListScreen extends Screen {
	private static final int ROW_H = 26;
	private static final int ROW_W = 290;

	private final Screen parent;
	private final List<List<AbstractWidget>> rows = new ArrayList<>();
	private int scroll = 0;

	public MacroListScreen(Screen parent) {
		super(Component.literal("Create Macro"));
		this.parent = parent;
	}

	private int top() {
		return 40;
	}

	private int bottom() {
		return this.height - 34;
	}

	@Override
	protected void init() {
		rows.clear();
		int cx = this.width / 2;
		int x0 = cx - ROW_W / 2;

		for (int i = 0; i < MacroConfig.macros.size(); i++) {
			final Macro macro = MacroConfig.macros.get(i);
			List<AbstractWidget> row = new ArrayList<>();

			String trig = macro.trigger == null ? "no key" : Keys.displayName(macro.trigger);
			Button edit = Button.builder(Component.literal(macro.name + "  [" + trig + "]"),
							b -> this.minecraft.setScreen(new MacroEditScreen(this, macro)))
					.bounds(x0, 0, 200, 20).build();

			Button toggle = Button.builder(stateLabel(macro), b -> {
				macro.enabled = !macro.enabled;
				b.setMessage(stateLabel(macro));
				MacroConfig.save();
			}).bounds(x0 + 204, 0, 56, 20).build();

			Button delete = Button.builder(Component.literal("X").withStyle(ChatFormatting.RED), b -> {
				MacroConfig.macros.remove(macro);
				MacroConfig.save();
				this.rebuildWidgets();
			}).bounds(x0 + 264, 0, 26, 20).build();

			row.add(edit);
			row.add(toggle);
			row.add(delete);
			for (AbstractWidget w : row) this.addRenderableWidget(w);
			rows.add(row);
		}

		this.addRenderableWidget(Button.builder(Component.literal("+ New macro"), b -> {
			Macro m = new Macro();
			m.name = "Macro " + (MacroConfig.macros.size() + 1);
			MacroConfig.macros.add(m);
			MacroConfig.save();
			this.minecraft.setScreen(new MacroEditScreen(this, m));
		}).bounds(cx - ROW_W / 2, this.height - 28, 142, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
				.bounds(cx + 4, this.height - 28, 142, 20).build());

		clampScroll();
	}

	private static Component stateLabel(Macro m) {
		return m.enabled
				? Component.literal("ON").withStyle(ChatFormatting.GREEN)
				: Component.literal("OFF").withStyle(ChatFormatting.RED);
	}

	private void clampScroll() {
		int viewport = bottom() - top();
		int max = Math.max(0, rows.size() * ROW_H - viewport);
		scroll = Math.max(0, Math.min(max, scroll));
	}

	private void layoutRows() {
		for (int i = 0; i < rows.size(); i++) {
			int y = top() + i * ROW_H - scroll;
			boolean visible = y >= top() && y + 20 <= bottom();
			for (AbstractWidget w : rows.get(i)) {
				w.setY(y);
				w.visible = visible;
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		layoutRows();
		super.render(g, mouseX, mouseY, partialTick);
		String title = "Create Macro";
		g.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, 16, 0xFFFFFFFF);
		if (MacroConfig.macros.isEmpty()) {
			String hint = "No macros yet - click \"+ New macro\"";
			g.drawString(this.font, hint, this.width / 2 - this.font.width(hint) / 2, top() + 10, 0xFFAAAAAA);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) (scrollY * 20);
		clampScroll();
		return true;
	}

	@Override
	public void onClose() {
		MacroConfig.save();
		this.minecraft.setScreen(parent);
	}
}
