package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.cosmetics.CosmeticKeyframe;
import com.boundingelf10.fec.cosmetics.CosmeticSlots;
import com.boundingelf10.fec.cosmetics.CosmeticSpec;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;

import java.util.List;
import java.util.UUID;

public final class CosmeticKeyframeUi {

	public interface Sink {
		void player(UUID player);

		void slot(String slot);

		void cosmetic(boolean fromReplay, CosmeticSpec spec);

		void duration(int ticks);
	}

	private static final String[] LABELS = {"Player", "Slot", "Cosmetic", "Duration"};

	private static final int[] SCRATCH = new int[1];
	private static final int[] DURATION = new int[1];

	private CosmeticKeyframeUi() {
	}

	public static UUID defaultPlayer() {
		return PlayerChoice.defaultPlayer();
	}

	public static void renderFields(CosmeticKeyframe values, String idSuffix, Sink sink) {
		Layout.pinWidth();

		float labelColumn = Math.max(Layout.labelColumn(LABELS), CosmeticSettingsUi.labelColumn());

		PlayerChoice.render("Cosmetic" + idSuffix, values.player, labelColumn, sink::player);
		renderSlot(values, idSuffix, labelColumn, sink);

		CosmeticSlot slot = values.slot();

		CosmeticPicker.render(
			"Cosmetic" + idSuffix,
			"Cosmetic",
			labelColumn,
			slot,
			values.fromReplay,
			values.fromReplay ? null : values.cosmetic.id,
			"From replay",
			CosmeticSlot.CAPE.equals(slot) ? "No cape" : "None",
			new CosmeticPicker.Sink() {
				@Override
				public void fromReplay() {
					sink.cosmetic(true, CosmeticSpec.nothing());
				}

				@Override
				public void nothing() {
					sink.cosmetic(false, CosmeticSpec.nothingIn(slot));
				}

				@Override
				public void cosmetic(Cosmetic cosmetic) {
					sink.cosmetic(false, CosmeticSpec.of(cosmetic.getId(), List.of()));
				}
			});

		if (values.fromReplay) {
			Layout.hint("Hands the slot back to whatever the replay recorded.");
		} else {
			CosmeticSettingsUi.render("Cosmetic" + idSuffix, slot, values.cosmetic, labelColumn,
				spec -> sink.cosmetic(false, spec));
		}

		renderDuration(values, idSuffix, labelColumn, sink);
	}

	private static void renderDuration(CosmeticKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		DURATION[0] = values.durationTicks;
		Layout.label("Duration", labelColumn, Layout.helpWidth());

		if (ImGuiHelper.inputInt("##fecCosmeticDuration" + idSuffix, DURATION)
			&& DURATION[0] != values.durationTicks) {
			sink.duration(Math.max(0, DURATION[0]));
		}

		Layout.help("How many ticks this keyframe holds the slot for. 0 holds it until the next marker.");
		Layout.hint(values.durationTicks > 0
			? "Holds for " + values.durationTicks + " ticks, then hands the slot back."
			: "Holds until the next marker.");
	}

	private static void renderSlot(CosmeticKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		List<CosmeticSlot> slots = CosmeticSlots.wearable();
		String[] labels = new String[slots.size()];
		int selected = 0;

		for (int i = 0; i < slots.size(); i++) {
			labels[i] = CosmeticSlots.displayNameOf(slots.get(i));

			if (slots.get(i).getId().equals(values.slot)) {
				selected = i;
			}
		}

		SCRATCH[0] = selected;
		Layout.label("Slot", labelColumn);

		if (ImGuiHelper.combo("##fecCosmeticSlot" + idSuffix, SCRATCH, labels) && SCRATCH[0] != selected) {
			sink.slot(slots.get(SCRATCH[0]).getId());
		}
	}
}
