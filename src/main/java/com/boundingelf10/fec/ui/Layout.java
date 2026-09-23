package com.boundingelf10.fec.ui;

import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.ReplayUI;

import imgui.moulberry90.ImGui;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public final class Layout {

	public static final float SECTION_WIDTH = 340.0F;
	private static final String HELP_MARKER = "(?)";

    private static float wrapEdge = 0.0F;

	private static final MethodHandle CONSUME_CONFIRM = replayUiFlag("consumeConfirm");
	private static final MethodHandle CONSUME_CANCEL = replayUiFlag("consumeCancel", "consumeNavClose");

	private Layout() { }

	public static boolean consumeConfirm() {
		return invokeFlag(CONSUME_CONFIRM);
	}

	public static boolean consumeCancel() {
		return invokeFlag(CONSUME_CANCEL);
	}

	private static MethodHandle replayUiFlag(String... names) {
		for (String name : names) {
			try {
				return MethodHandles.publicLookup().findStatic(ReplayUI.class, name, MethodType.methodType(boolean.class));
			} catch (ReflectiveOperationException ignored) {
			}
		}

		return null;
	}

	private static boolean invokeFlag(MethodHandle handle) {
		try {
			return handle != null && (boolean) handle.invokeExact();
		} catch (Throwable t) {
			return false;
		}
	}

	public static float labelColumn(String... labels) {
		return labelColumn(List.of(labels));
	}

	public static float labelColumn(List<String> labels) {
		float widest = 0.0F;

		for (String label : labels) {
			widest = Math.max(widest, ImGuiHelper.calcTextWidth(label));
		}

		return widest + ImGui.getStyle().getItemSpacingX() * 2.0F;
	}

	public static float indented(float column) {
		return column - ImGui.getStyle().getIndentSpacing();
	}

	public static void pinWidth() {
		pinWidth(SECTION_WIDTH);
	}

	public static void pinWidth(float width) {
		float cursorY = ImGui.getCursorPosY();

		wrapEdge = ImGui.getCursorPosX() + width;
		ImGui.dummy(width, 0.0F);
		ImGui.setCursorPosY(cursorY);
	}

	public static void fillWidth() {
		wrapEdge = 0.0F;
	}

	public static float label(String label, float column) {
		return label(label, column, 0.0F);
	}

	public static float label(String label, float column, float trailing) {
		float startX = ImGui.getCursorPosX();

		ImGui.alignTextToFramePadding();
		ImGui.textUnformatted(label);
		ImGui.sameLine();
		ImGui.setCursorPosX(Math.max(ImGui.getCursorPosX(), startX + column));
		ImGui.setNextItemWidth(trailing > 0.0F ? -(trailing + ImGui.getStyle().getItemSpacingX()) : -1.0F);

		return ImGui.calcItemWidth();
	}

	public static float helpWidth() {
		return ImGuiHelper.calcTextWidth(HELP_MARKER);
	}

	public static void help(String message) {
		ImGui.sameLine();
		ImGuiHelper.helpMarker(message);
	}

	public static void helpRight(String message) {
		ImGui.sameLine();

		float free = ImGui.getContentRegionAvailX() - helpWidth();

		if (free > 0.0F) {
			ImGui.setCursorPosX(ImGui.getCursorPosX() + free);
		}

		ImGuiHelper.helpMarker(message);
	}

	public static int buttonRow(String... labels) {
		return buttonRow(null, labels);
	}

	public static int buttonRow(boolean[] enabled, String... labels) {
		float spacing = ImGui.getStyle().getItemSpacingX();
		float startX = ImGui.getCursorPosX();
		float width = Math.max(1.0F,
			(float) Math.floor((ImGui.getContentRegionAvailX() - spacing * (labels.length - 1)) / labels.length));
		int pressed = -1;

		for (int i = 0; i < labels.length; i++) {
			if (i > 0) {
				ImGui.sameLine();
				ImGui.setCursorPosX(startX + (width + spacing) * i);
			}

			ImGui.beginDisabled(enabled != null && !enabled[i]);

			if (ImGui.button(labels[i], width, 0.0F)) {
				pressed = i;
			}

			ImGui.endDisabled();
		}

		return pressed;
	}

	public static boolean wideButton(String label) {
		return ImGui.button(label, ImGui.getContentRegionAvailX(), 0.0F);
	}

	public static void hint(String text) {
		ImGui.pushTextWrapPos(wrapEdge);
		ImGui.textDisabled(text);
		ImGui.popTextWrapPos();
	}

	public static void warning(String text) {
		ImGui.pushTextWrapPos(wrapEdge);
		ImGui.textColored(1.0F, 0.7F, 0.3F, 1.0F, text);
		ImGui.popTextWrapPos();
	}

	public static void rightAlignedHint(String text) {
		ImGui.sameLine();

		float free = ImGui.getContentRegionAvailX() - ImGuiHelper.calcTextWidth(text);

		if (free > 0.0F) {
			ImGui.setCursorPosX(ImGui.getCursorPosX() + free);
		}

		ImGui.textDisabled(text);
	}
}
