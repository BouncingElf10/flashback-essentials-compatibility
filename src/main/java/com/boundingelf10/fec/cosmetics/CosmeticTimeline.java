package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.boundingelf10.fec.state.Scenes;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.KeyframeTrack;

import gg.essential.mod.cosmetics.CosmeticSlot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public final class CosmeticTimeline {
	public record Decision(@Nullable CosmeticSpec spec) { }

	private CosmeticTimeline() { }

	public static Map<UUID, Map<CosmeticSlot, Decision>> collect(EditorState editorState, int tick, long stamp) {
		return Scenes.read(editorState, stamp, scene -> collect(scene, tick));
	}

	private static Map<UUID, Map<CosmeticSlot, Decision>> collect(EditorScene scene, int tick) {
		Map<UUID, Map<CosmeticSlot, Decision>> wanted = new LinkedHashMap<>();

		for (KeyframeTrack track : scene.keyframeTracks) {
			if (!track.enabled || track.keyframeType != CosmeticKeyframeType.INSTANCE) {
				continue;
			}

			Map.Entry<Integer, Keyframe> entry = track.keyframesByTick.floorEntry(tick);

			if (entry == null || !(entry.getValue() instanceof CosmeticKeyframe cosmetic)) {
				continue;
			}

			if (EmoteKeyframe.NO_PLAYER.equals(cosmetic.player)) {
				continue;
			}

			int start = entry.getKey();
			Map.Entry<Integer, Keyframe> next = track.keyframesByTick.higherEntry(start);

			if (tick >= cosmetic.endTick(start, next == null ? null : next.getKey())) {
				continue;
			}

			wanted.computeIfAbsent(cosmetic.player, ignored -> new LinkedHashMap<>())
				.put(cosmetic.slot(), new Decision(cosmetic.fromReplay ? null : cosmetic.cosmetic));
		}

		return wanted;
	}
}
