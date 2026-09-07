package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.cosmetics.EssentialCosmetics;
import com.boundingelf10.fec.cosmetics.RecordedCosmetics;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.boundingelf10.fec.state.EssentialState;
import com.boundingelf10.fec.state.Scenes;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.ext.MinecraftExt;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.playback.ReplayServer;
import com.moulberry.flashback.state.EditorScene;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.moulberry.flashback.state.KeyframeTrack;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import org.jetbrains.annotations.Nullable;


public final class EmoteTimeline {

	private record Forced(@Nullable EquippedCosmeticId equipped, int startTick, @Nullable String emoteId,
		@Nullable String animationVariant, float animationTicks) { }

	private static final Map<UUID, Forced> FORCED = new LinkedHashMap<>();

	private static ClientPacketListener lastConnection = null;
	private static int lastTick = -1;
	private static float lastAppliedTick = Float.NaN;
	private static boolean warned = false;
	private static boolean warnedHolding = false;

	private EmoteTimeline() { }

	public static boolean controls(UUID player) {
		return FORCED.containsKey(player);
	}

	@Nullable
	public static EquippedCosmeticId forced(UUID player) {
		Forced forced = FORCED.get(player);
		return forced == null ? null : forced.equipped();
	}

	public static void tick() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection != lastConnection) {
			lastConnection = connection;
			reset();
			return;
		}

		if (!Flashback.isInReplay()) {
			reset();
		}
	}

	private static void reset() {
		FORCED.clear();
		lastAppliedTick = Float.NaN;
		EmoteScrub.forget();
		lastTick = -1;
	}

	public static void apply(EditorState editorState, float tick, long stamp) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection == null || !Flashback.isInReplay()) {
			return;
		}

		lastAppliedTick = tick;

		try {
			int currentTick = (int) tick;
			Map<UUID, Forced> wanted = collect(editorState, tick, stamp);

			if (wanted.isEmpty() && FORCED.isEmpty()) {
				lastTick = currentTick;
				return;
			}

			IngameEquippedOutfitsManager manager = EssentialCosmetics.ingameManagerFor(connection);

			if (manager == null) {
				return;
			}

			boolean rewound = lastTick >= 0 && currentTick < lastTick;
			lastTick = currentTick;

			for (Map.Entry<UUID, Forced> entry : wanted.entrySet()) {
				equip(manager, entry.getKey(), entry.getValue(), rewound);
			}

			for (UUID player : List.copyOf(FORCED.keySet())) {
				if (!wanted.containsKey(player)) {
					FORCED.remove(player);
					set(manager, player, RecordedCosmetics.get(player, CosmeticSlot.EMOTE));
				}
			}

			warned = false;
		} catch (Exception e) {
			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to apply Essential emote keyframes", e);
			}

			reset();
		}
	}

	public static void hold(DeltaTracker deltaTracker) {
		if (!Flashback.isInReplay()) {
			return;
		}

		followPlayhead();

		if (FORCED.isEmpty()) {
			return;
		}

		try {
			List<UUID> rebuild = null;

			for (Map.Entry<UUID, Forced> entry : FORCED.entrySet()) {
				Forced forced = entry.getValue();

				if (forced.emoteId() == null || forced.equipped() == null) {
					continue;
				}

				UUID player = entry.getKey();
				float seconds = forced.animationTicks() / 20.0F;
				EmoteScrub.Result result = EmoteScrub.seek(player, forced.emoteId(), seconds, deltaTracker);

				if (result == EmoteScrub.Result.NOT_PLAYING
					&& EmoteScrub.restart(player, forced.emoteId(), forced.animationVariant())) {
					result = EmoteScrub.seek(player, forced.emoteId(), seconds, deltaTracker);
				}

				if (result == EmoteScrub.Result.REWOUND) {
					rebuild = rebuild == null ? new ArrayList<>(1) : rebuild;
					rebuild.add(player);
				}
			}

			if (rebuild != null) {
				rebuild(rebuild);
			}

			warnedHolding = false;
		} catch (Exception e) {
			if (!warnedHolding) {
				warnedHolding = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to hold Essential emotes on the playhead", e);
			}
		}
	}

	private static void followPlayhead() {
		ReplayServer replayServer = Flashback.getReplayServer();

		if (replayServer == null) {
			return;
		}

		float tick = (float) replayServer.getPartialReplayTick();

		if (tick == lastAppliedTick) {
			return;
		}

		EditorState editorState = EditorStateManager.getCurrent();

		if (editorState != null) {
			apply(editorState, tick, 0L);
		}
	}

	private static void rebuild(List<UUID> players) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		IngameEquippedOutfitsManager manager = connection == null
			? null : EssentialCosmetics.ingameManagerFor(connection);

		if (manager == null) {
			return;
		}

		for (UUID player : players) {
			Forced forced = FORCED.get(player);

			if (forced == null) {
				continue;
			}


			FORCED.put(player, new Forced(null, forced.startTick(), forced.emoteId(),
				forced.animationVariant(), forced.animationTicks()));
			set(manager, player, null);
		}

		((MinecraftExt) Minecraft.getInstance()).flashback$applyKeyframes();
	}

	private static Map<UUID, Forced> collect(EditorState editorState, float tick, long stamp) {
		EssentialReplayState state = EssentialState.of(editorState);

		if (state.hideEmotes || state.hideAllCosmetics) {
			return suppressEveryone();
		}

		Map<UUID, Forced> wanted = Scenes.read(editorState, stamp, scene -> collect(scene, tick));

		for (String key : state.hidden()) {
			try {
				wanted.put(UUID.fromString(key), new Forced(null, (int) tick, null, null, 0.0F));
			} catch (IllegalArgumentException ignored) {

			}
		}

		return wanted;
	}

	private static Map<UUID, Forced> suppressEveryone() {
		Map<UUID, Forced> wanted = new LinkedHashMap<>();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection == null) {
			return wanted;
		}

		for (UUID player : EssentialCosmetics.knownPlayers(connection, Minecraft.getInstance().level)) {
			wanted.put(player, new Forced(null, 0, null, null, 0.0F));
		}

		return wanted;
	}

	private static Map<UUID, Forced> collect(EditorScene scene, float tick) {
		Map<UUID, Forced> wanted = new LinkedHashMap<>();
		int currentTick = (int) tick;

		for (KeyframeTrack track : scene.keyframeTracks) {
			if (!track.enabled || track.keyframeType != EmoteKeyframeType.INSTANCE) {
				continue;
			}

			Map.Entry<Integer, Keyframe> entry = track.keyframesByTick.floorEntry(currentTick);

			if (entry == null || !(entry.getValue() instanceof EmoteKeyframe emote)) {
				continue;
			}

			int start = entry.getKey();
			Map.Entry<Integer, Keyframe> next = track.keyframesByTick.higherEntry(start);
			int end = next == null ? Integer.MAX_VALUE : next.getKey();
			int duration = emote.resolvedDurationTicks();

			if (duration > 0) {
				end = Math.min(end, start + duration);
			}

			if (currentTick < end && !EmoteKeyframe.NO_PLAYER.equals(emote.player)) {
				wanted.put(emote.player, new Forced(emote.equipped(), start, emote.emoteId,
					emote.animationVariant, emote.animationTicksAt(tick, start)));
			}
		}

		return wanted;
	}

	private static void equip(IngameEquippedOutfitsManager manager, UUID player, Forced wanted, boolean rewound) {
		Forced have = FORCED.get(player);

		if (!EmoteScrub.isAvailable()) {
			boolean sameEmote = have != null
				&& wanted.equipped() != null
				&& Objects.equals(have.equipped(), wanted.equipped());

			if (sameEmote
				&& (rewound || have.startTick() != wanted.startTick())
				&& !EmoteScrub.isPlaying(player, wanted.emoteId())) {
				FORCED.put(player, new Forced(null, wanted.startTick(), wanted.emoteId(),
					wanted.animationVariant(), wanted.animationTicks()));
				set(manager, player, null);
				((MinecraftExt) Minecraft.getInstance()).flashback$applyKeyframes();
				return;
			}
		}

		FORCED.put(player, wanted);
		set(manager, player, wanted.equipped());
	}

	private static void set(IngameEquippedOutfitsManager manager, UUID player, @Nullable EquippedCosmeticId equipped) {
		Outfit outfit = EssentialCosmetics.outfitOf(manager, player);

		if (Objects.equals(outfit.getCosmetics().get(CosmeticSlot.EMOTE), equipped)) {
			return;
		}

		List<Update> updates = new ArrayList<>(1);
		updates.add(new Update.Cosmetic(CosmeticSlot.EMOTE, equipped));
		manager.applyUpdates(player, updates);
	}
}
