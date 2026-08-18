package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.cosmetics.CosmeticCatalog;
import com.boundingelf10.fec.cosmetics.CosmeticSettings;
import com.boundingelf10.fec.cosmetics.CosmeticSpec;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticProperty;
import gg.essential.model.Side;
import gg.essential.network.cosmetics.Cosmetic;

import imgui.moulberry90.ImGui;

import java.util.List;
import java.util.function.Consumer;

public final class CosmeticSettingsUi {

	private static final Side[] SIDES = {Side.LEFT, Side.RIGHT, Side.FRONT, Side.BACK};
	private static final String[] SIDE_LABELS = {"Left", "Right", "Front", "Back"};

	private static final String[] LABELS = {"Colour", "Side", "X", "Y", "Z"};

	private static final int[] SCRATCH = new int[1];
	private static final float[] POS_X = new float[1];
	private static final float[] POS_Y = new float[1];
	private static final float[] POS_Z = new float[1];

	private CosmeticSettingsUi() {
	}

    public static float labelColumn() {
		return Layout.labelColumn(LABELS) + ImGui.getStyle().getIndentSpacing();
	}

	public static void render(String id, CosmeticSlot slot, CosmeticSpec spec, float labelColumn, Consumer<CosmeticSpec> sink) {
		if (spec.isNothing()) {
			return;
		}

		Cosmetic cosmetic = CosmeticCatalog.find(slot, spec.id);

		if (cosmetic == null) {
			return;
		}

		float column = Math.max(Layout.indented(labelColumn), Layout.labelColumn(LABELS));

		ImGui.indent();
		renderVariant(id, cosmetic, spec, column, sink);
		renderSide(id, cosmetic, spec, column, sink);
		renderPosition(id, cosmetic, spec, column, sink);
		ImGui.unindent();
	}

	private static void renderVariant(String id, Cosmetic cosmetic, CosmeticSpec spec, float column, Consumer<CosmeticSpec> sink) {
		List<CosmeticProperty.Variants.Variant> variants = CosmeticSettings.variantsOf(cosmetic);

		if (variants.isEmpty()) {
			return;
		}

		String current = spec.variant == null ? cosmetic.getDefaultVariantName() : spec.variant;
		String[] names = new String[variants.size()];
		int selected = 0;

		for (int i = 0; i < variants.size(); i++) {
			names[i] = variants.get(i).getName();

			if (names[i].equals(current)) {
				selected = i;
			}
		}

		SCRATCH[0] = selected;
		Layout.label("Colour", column);

		if (ImGuiHelper.combo("##fecVariant" + id, SCRATCH, names) && SCRATCH[0] != selected) {
			CosmeticSpec updated = spec.copy();
			updated.variant = names[SCRATCH[0]];
			sink.accept(updated);
		}
	}

	private static void renderSide(String id, Cosmetic cosmetic, CosmeticSpec spec, float column, Consumer<CosmeticSpec> sink) {
		if (!CosmeticSettings.supportsSides(cosmetic)) {
			return;
		}

		Side current = spec.sideValue();

		if (current == null) {
			current = cosmetic.getDefaultSide();
		}

		int selected = 0;

		for (int i = 0; i < SIDES.length; i++) {
			if (SIDES[i] == current) {
				selected = i;
			}
		}

		SCRATCH[0] = selected;
		Layout.label("Side", column);

		if (ImGuiHelper.combo("##fecSide" + id, SCRATCH, SIDE_LABELS) && SCRATCH[0] != selected) {
			CosmeticSpec updated = spec.copy();
			updated.side = SIDES[SCRATCH[0]].name();
			sink.accept(updated);
		}
	}

	private static void renderPosition(String id, Cosmetic cosmetic, CosmeticSpec spec, float column, Consumer<CosmeticSpec> sink) {
		CosmeticProperty.PositionRange.Data range = CosmeticSettings.positionRangeOf(cosmetic);

		if (range == null) {
			return;
		}

		POS_X[0] = spec.x == null ? 0.0F : spec.x;
		POS_Y[0] = spec.y == null ? 0.0F : spec.y;
		POS_Z[0] = spec.z == null ? 0.0F : spec.z;

		Layout.label("X", column);
		boolean changed = ImGui.dragFloat("##fecPosX" + id, POS_X, 0.05F,
			CosmeticSettings.rangeMin(range.getXMin()), CosmeticSettings.rangeMax(range.getXMax()));
		Layout.label("Y", column);
		changed |= ImGui.dragFloat("##fecPosY" + id, POS_Y, 0.05F,
			CosmeticSettings.rangeMin(range.getYMin()), CosmeticSettings.rangeMax(range.getYMax()));
		Layout.label("Z", column);
		changed |= ImGui.dragFloat("##fecPosZ" + id, POS_Z, 0.05F,
			CosmeticSettings.rangeMin(range.getZMin()), CosmeticSettings.rangeMax(range.getZMax()));

		if (changed) {
			CosmeticSpec updated = spec.copy();
			updated.x = POS_X[0];
			updated.y = POS_Y[0];
			updated.z = POS_Z[0];
			sink.accept(updated);
		}
	}
}
