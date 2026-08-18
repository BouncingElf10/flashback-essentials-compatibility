package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.cosmetics.CosmeticSettings;
import com.boundingelf10.fec.ui.CosmeticPicker;
import com.boundingelf10.fec.ui.Layout;
import com.boundingelf10.fec.ui.PlayerChoice;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticProperty;
import gg.essential.network.cosmetics.Cosmetic;

import imgui.moulberry90.ImGui;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public final class EmoteKeyframeUi {

	public interface Sink {
		void player(UUID player);

		void emote(@Nullable String emoteId);

		void variant(@Nullable String variant);

		void animationVariant(@Nullable String animationVariant);

		void duration(int ticks);

		void speed(float speed);

		void freeze(int ticks);
	}

	private static final String RANDOM_VARIATION = "Random";

	private static final String[] LABELS = {"Player", "Emote", "Colour", "Variation", "Speed", "Freeze", "Frozen at", "Duration"};

	private static final int[] SCRATCH = new int[1];
	private static final int[] DURATION = new int[1];
	private static final int[] FREEZE = new int[1];
	private static final float[] SPEED = new float[1];

	private EmoteKeyframeUi() {
	}

	public static void renderFields(EmoteKeyframe values, String idSuffix, Sink sink) {
		Layout.pinWidth();

		float labelColumn = Layout.labelColumn(LABELS);

		PlayerChoice.render("Emote" + idSuffix, values.player, labelColumn, sink::player);
		renderEmote(values, idSuffix, labelColumn, sink);

		Cosmetic cosmetic = EmoteCatalog.find(values.emoteId);

		if (cosmetic != null) {
			renderVariant(values, cosmetic, idSuffix, labelColumn, sink);
			renderAnimationVariant(values, idSuffix, labelColumn, sink);
			renderTiming(values, idSuffix, labelColumn, sink);
		}

		renderDuration(values, idSuffix, labelColumn, sink);
	}

	private static void renderEmote(EmoteKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		CosmeticPicker.render(
			"Emote" + idSuffix,
			"Emote",
			labelColumn,
			CosmeticSlot.EMOTE,
			false,
			values.emoteId,
			null,
			"No emote",
			new CosmeticPicker.Sink() {
				@Override
				public void fromReplay() {
					sink.emote(null);
				}

				@Override
				public void nothing() {
					sink.emote(null);
				}

				@Override
				public void cosmetic(Cosmetic cosmetic) {
					sink.emote(cosmetic.getId());
				}
			});

		if (values.emoteId == null) {
			Layout.hint("Suppresses whatever emote the replay plays here.");
		}
	}

	private static void renderVariant(EmoteKeyframe values, Cosmetic cosmetic, String idSuffix, float labelColumn, Sink sink) {
		List<CosmeticProperty.Variants.Variant> variants = CosmeticSettings.variantsOf(cosmetic);

		if (variants.isEmpty()) {
			return;
		}

		String current = values.variant == null ? cosmetic.getDefaultVariantName() : values.variant;
		String[] names = new String[variants.size()];
		int selected = 0;

		for (int i = 0; i < variants.size(); i++) {
			names[i] = variants.get(i).getName();

			if (names[i].equals(current)) {
				selected = i;
			}
		}

		SCRATCH[0] = selected;
		Layout.label("Colour", labelColumn);

		if (ImGuiHelper.combo("##fecEmoteVariant" + idSuffix, SCRATCH, names) && SCRATCH[0] != selected) {
			sink.variant(names[SCRATCH[0]]);
		}
	}

	private static void renderAnimationVariant(EmoteKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		EmoteCatalog.Info info = EmoteCatalog.infoOf(values.emoteId, values.variant, values.animationVariant);

		if (info == null || info.animationVariants().size() < 2) {
			return;
		}

		List<String> variations = info.animationVariants();
		String[] labels = new String[variations.size() + 1];
		labels[0] = RANDOM_VARIATION;

		for (int i = 0; i < variations.size(); i++) {
			labels[i + 1] = variations.get(i);
		}

		int selected = Math.max(0, variations.indexOf(values.animationVariant) + 1);
		SCRATCH[0] = selected;
		Layout.label("Variation", labelColumn);

		if (ImGuiHelper.combo("##fecEmoteAnimation" + idSuffix, SCRATCH, labels) && SCRATCH[0] != selected) {
			sink.animationVariant(SCRATCH[0] == 0 ? null : variations.get(SCRATCH[0] - 1));
		}
	}

	private static void renderTiming(EmoteKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		SPEED[0] = values.resolvedSpeed();
		Layout.label("Speed", labelColumn, Layout.helpWidth());

		if (ImGui.dragFloat("##fecEmoteSpeed" + idSuffix, SPEED, 0.01F, 0.05F, 5.0F) && SPEED[0] != values.speed) {
			sink.speed(Math.max(0.05F, SPEED[0]));
		}

		Layout.help("How fast the emote plays, independent of the replay's timescale.");

		boolean frozen = values.isFrozen();
		Layout.label("Freeze", labelColumn);

		if (ImGui.checkbox("##fecEmoteFreezeToggle" + idSuffix, frozen)) {
			sink.freeze(frozen ? -1 : 0);
		}

		Layout.helpRight("Holds the emote on one frame, for posing a player in a still shot.");

		if (!frozen) {
			return;
		}

		FREEZE[0] = values.freezeTicks;
		Layout.label("Frozen at", labelColumn, Layout.helpWidth());

		if (ImGuiHelper.inputInt("##fecEmoteFreeze" + idSuffix, FREEZE) && FREEZE[0] != values.freezeTicks) {
			sink.freeze(Math.max(0, FREEZE[0]));
		}

		EmoteCatalog.Info info = EmoteCatalog.infoOf(values.emoteId, values.variant, values.animationVariant);
		Layout.hint(info == null
			? "Ticks into the emote."
			: "Ticks into the emote, of " + info.lengthTicks() + ".");
	}

	private static void renderDuration(EmoteKeyframe values, String idSuffix, float labelColumn, Sink sink) {
		DURATION[0] = values.durationTicks;
		Layout.label("Duration", labelColumn, Layout.helpWidth());

		if (ImGuiHelper.inputInt("##fecEmoteDuration" + idSuffix, DURATION) && DURATION[0] != values.durationTicks) {
			sink.duration(Math.max(0, DURATION[0]));
		}

		Layout.help("How many ticks the emote is held for. 0 works it out from the emote itself.");
		Layout.hint(describeDuration(values));
	}

	private static String describeDuration(EmoteKeyframe values) {
		if (values.durationTicks > 0) {
			return "Holds for " + values.durationTicks + " ticks, or until the next marker.";
		}

		if (values.emoteId == null) {
			return "Holds until the next marker.";
		}

		if (values.isFrozen()) {
			return "Frozen, so it holds until the next marker.";
		}

		EmoteCatalog.Info info = EmoteCatalog.infoOf(values.emoteId, values.variant, values.animationVariant);

		if (info == null) {
			return "Still loading this emote; holding until the next marker.";
		}

		if (info.loops()) {
			return "This emote loops, so it holds until the next marker.";
		}

		return "Plays once over " + values.resolvedDurationTicks() + " ticks.";
	}

	public static UUID defaultPlayer() {
		return PlayerChoice.defaultPlayer();
	}
}
