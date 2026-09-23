package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.boundingelf10.fec.ui.CosmeticKeyframeUi;
import com.boundingelf10.fec.ui.Layout;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;

import gg.essential.mod.cosmetics.CosmeticSlot;

import imgui.moulberry90.ImGui;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class CosmeticKeyframeType implements KeyframeType<CosmeticKeyframe> {

	public static final CosmeticKeyframeType INSTANCE = new CosmeticKeyframeType();
	public static final char ICON = 0xE7FC;

	private CosmeticKeyframeType() { }

	@Override
	@Nullable
	public Class<? extends KeyframeChange> keyframeChangeType() {
		return null;
	}

	@Override
	@Nullable
	public String icon() {
		return String.valueOf(ICON);
	}

	@Override
	public String name() {
		return "Essential Cosmetic";
	}

	@Override
	public String id() {
		return "FEC_ESSENTIAL_COSMETIC";
	}

	@Override
	public boolean allowChangingInterpolationType() {
		return false;
	}

	@Override
	public boolean cullKeyframesInTimelineToTheLeft() {
		return false;
	}

	@Override
	@Nullable
	public CosmeticKeyframe createDirect() {
		return null;
	}

	@Override
	public KeyframeType.KeyframeCreatePopup<CosmeticKeyframe> createPopup() {
		CosmeticKeyframe draft = new CosmeticKeyframe(
			CosmeticKeyframeUi.defaultPlayer(), CosmeticSlot.HAT.getId(), true, CosmeticSpec.nothing());

		return () -> {
			CosmeticKeyframeUi.renderFields(draft, "Create", new CosmeticKeyframeUi.Sink() {
				@Override
				public void player(UUID player) {
					draft.player = player;
				}

				@Override
				public void slot(String slot) {
					draft.slot = slot;
					draft.cosmetic = CosmeticSpec.nothing();
					draft.fromReplay = true;
				}

				@Override
				public void cosmetic(boolean fromReplay, CosmeticSpec spec) {
					draft.fromReplay = fromReplay;
					draft.cosmetic = spec;
				}

				@Override
				public void duration(int ticks) {
					draft.durationTicks = ticks;
				}
			});

			boolean ready = !EmoteKeyframe.NO_PLAYER.equals(draft.player);

			if (!ready) {
				ImGui.beginDisabled();
			}

			boolean add = ImGui.button("Add##fecCosmeticAdd") || Layout.consumeConfirm();

			if (!ready) {
				ImGui.endDisabled();
			}

			ImGui.sameLine();

			if (ImGui.button("Cancel##fecCosmeticCancel") || Layout.consumeCancel()) {
				ImGui.closeCurrentPopup();
				return null;
			}

			return add && ready ? (CosmeticKeyframe) draft.copy() : null;
		};
	}
}
