package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.ui.Layout;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;

import imgui.moulberry90.ImGui;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class EmoteKeyframeType implements KeyframeType<EmoteKeyframe> {

	public static final EmoteKeyframeType INSTANCE = new EmoteKeyframeType();
	public static final char ICON = 0xE766;

	private EmoteKeyframeType() { }

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
		return "Essential Emote";
	}

	@Override
	public String id() {
		return "FEC_ESSENTIAL_EMOTE";
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
	public EmoteKeyframe createDirect() {
		return null;
	}

	@Override
	public KeyframeType.KeyframeCreatePopup<EmoteKeyframe> createPopup() {
		EmoteKeyframe draft = new EmoteKeyframe(EmoteKeyframeUi.defaultPlayer(), null, null, null, 0);

		return () -> {
			EmoteKeyframeUi.renderFields(draft, "Create", new EmoteKeyframeUi.Sink() {
				@Override
				public void player(UUID player) {
					draft.player = player;
				}

				@Override
				public void emote(@Nullable String emoteId) {
					draft.emoteId = emoteId;
					draft.variant = null;
					draft.animationVariant = null;
				}

				@Override
				public void variant(@Nullable String variant) {
					draft.variant = variant;
				}

				@Override
				public void animationVariant(@Nullable String animationVariant) {
					draft.animationVariant = animationVariant;
				}

				@Override
				public void duration(int ticks) {
					draft.durationTicks = ticks;
				}

				@Override
				public void speed(float speed) {
					draft.speed = speed;
				}

				@Override
				public void freeze(int ticks) {
					draft.freezeTicks = ticks;
				}
			});

			boolean ready = !EmoteKeyframe.NO_PLAYER.equals(draft.player);

			if (!ready) {
				ImGui.beginDisabled();
			}

			boolean add = ImGui.button("Add##fecEmoteAdd") || Layout.consumeConfirm();

			if (!ready) {
				ImGui.endDisabled();
			}

			ImGui.sameLine();

			if (ImGui.button("Cancel##fecEmoteCancel") || Layout.consumeCancel()) {
				ImGui.closeCurrentPopup();
				return null;
			}

			return add && ready ? (EmoteKeyframe) draft.copy() : null;
		};
	}
}
