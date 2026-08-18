package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;

import gg.essential.cosmetics.WearablesManager;
import gg.essential.cosmetics.events.AnimationEvent;
import gg.essential.cosmetics.events.AnimationEventType;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.model.Animation;
import gg.essential.model.ModelAnimationState.AnimationState;
import gg.essential.model.ModelInstance;
import gg.essential.model.file.AnimationFile;
import gg.essential.network.cosmetics.Cosmetic;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

public final class EmoteScrub {
	public enum Result {
		SEEKED,
		NO_MODEL,
		NOT_PLAYING,
		REWOUND,
		UNAVAILABLE
	}

	private record Playing(String emoteId, String animation) { }
	private record Link(float offset, float duration, boolean last) { }

	private static final float SEEK_THRESHOLD_SECONDS = 0.15F;
	private static final float END_MARGIN_SECONDS = 0.005F;
	private static final Link UNKNOWN = new Link(0.0F, 0.0F, false);
	private static final Map<UUID, Playing> LAST_PLAYED = new HashMap<>();

	private static Field animStartTime = null;
	private static boolean unavailable = false;
	private static boolean warned = false;

	private EmoteScrub() { }

	public static boolean isAvailable() {
		return !unavailable;
	}

	public static void forget() {
		LAST_PLAYED.clear();
	}

	public static boolean isPlaying(UUID player, @Nullable String emoteId) {
		ModelInstance model = modelOf(player, emoteId);
		return model != null && !model.getAnimationState().getActive().isEmpty();
	}

	public static Result seek(UUID player, String emoteId, float animTimeSeconds, DeltaTracker deltaTracker) {
		if (unavailable) {
			return Result.UNAVAILABLE;
		}

		Player entity = playerOf(player);
		ModelInstance model = modelOf(entity, emoteId);

		if (model == null) {
			return Result.NO_MODEL;
		}

		List<AnimationState> active = model.getAnimationState().getActive();

		if (active.isEmpty()) {
			return Result.NOT_PLAYING;
		}

		float lifeTime = lifeTimeOf(entity, deltaTracker);
		float wanted = Math.max(0.0F, animTimeSeconds);
		Result result = Result.SEEKED;

		for (AnimationState state : active) {
			String name = state.getAnimation().getName();
			Link link = linkOf(model, name);

			float local = wanted - link.offset();

			if (local < 0.0F) {
				local = 0.0F;
				result = Result.REWOUND;
			} else if (link.last() && link.duration() > 0.0F) {
				local = Math.min(local, Math.max(0.0F, link.duration() - END_MARGIN_SECONDS));
			}

			float current = lifeTime - state.getAnimStartTime();

			if (!setStart(state, lifeTime - local)) {
				return Result.UNAVAILABLE;
			}

			if (Math.abs(current - local) > SEEK_THRESHOLD_SECONDS) {
				resyncEffects(state, local);
			}

			LAST_PLAYED.put(player, new Playing(emoteId, name));
		}

		return result;
	}

	private static Link linkOf(ModelInstance model, String animationName) {
		for (AnimationEvent root : model.getModel().getAnimationEvents()) {
			if (root.getType() != AnimationEventType.EMOTE) {
				continue;
			}

			float offset = 0.0F;

			for (AnimationEvent link = root; link != null; link = link.getOnComplete()) {
				float duration = durationOf(model, link);

				if (link.getName().equals(animationName)) {
					return new Link(offset, duration, link.getOnComplete() == null);
				}

				if (duration <= 0.0F) {
					break;
				}

				offset += duration;
			}
		}

		return UNKNOWN;
	}

	private static float durationOf(ModelInstance model, AnimationEvent event) {
		if (event.getLoops() <= 0) {
			return 0.0F;
		}

		Animation animation = model.getModel().getAnimationByName(event.getName());
		return animation == null ? 0.0F : animation.getAnimationLength() * event.getLoops();
	}

	public static boolean restart(UUID player, String emoteId, @Nullable String animationVariant) {
		if (unavailable) {
			return false;
		}

		ModelInstance model = modelOf(playerOf(player), emoteId);

		if (model == null) {
			return false;
		}

		String animation = animationVariant != null ? animationVariant : animationToReplay(player, emoteId, model);

		if (animation == null) {
			return false;
		}

		model.getEssentialAnimationSystem().fireTriggerFromAnimation(animation, AnimationEventType.EMOTE);
		return !model.getAnimationState().getActive().isEmpty();
	}

	@Nullable
	private static String animationToReplay(UUID player, String emoteId, ModelInstance model) {
		Playing playing = LAST_PLAYED.get(player);
		String seen = playing != null && playing.emoteId().equals(emoteId) ? playing.animation() : null;
		String first = null;

		for (AnimationEvent root : model.getModel().getAnimationEvents()) {
			if (root.getType() != AnimationEventType.EMOTE) {
				continue;
			}

			if (first == null) {
				first = root.getName();
			}

			for (AnimationEvent link = root; link != null; link = link.getOnComplete()) {
				if (link.getName().equals(seen)) {
					return root.getName();
				}
			}
		}

		return first;
	}

	private static float lifeTimeOf(Player player, DeltaTracker deltaTracker) {
		int ticks = player.tickCount;

		if (ticks <= 1) {
			return ticks / 20.0F;
		}

		return (ticks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 20.0F;
	}

	private static void resyncEffects(AnimationState state, float animTimeSeconds) {
		float length = state.getAnimation().getAnimationLength();

		if (state.getAnimation().getLoop() == AnimationFile.Loop.True && length > 0.0F) {
			state.setEffectLoops$cosmetics((int) (animTimeSeconds / length));
			state.setLastEffectTime$cosmetics(animTimeSeconds % length);
		} else {
			state.setEffectLoops$cosmetics(0);
			state.setLastEffectTime$cosmetics(animTimeSeconds);
		}
	}

	@Nullable
	private static Player playerOf(UUID uuid) {
		ClientLevel level = Minecraft.getInstance().level;
		return level == null ? null : level.getPlayerByUUID(uuid);
	}

	@Nullable
	private static ModelInstance modelOf(UUID uuid, @Nullable String emoteId) {
		return modelOf(playerOf(uuid), emoteId);
	}

	@Nullable
	private static ModelInstance modelOf(@Nullable Player player, @Nullable String emoteId) {
		if (emoteId == null || !(player instanceof AbstractClientPlayerExt ext)) {
			return null;
		}

		WearablesManager wearables = ext.getWearablesManager();

		if (wearables == null) {
			return null;
		}

		for (Map.Entry<Cosmetic, ModelInstance> entry : wearables.getModels().entrySet()) {
			if (entry.getKey().getId().equals(emoteId)) {
				return entry.getValue();
			}
		}

		return null;
	}

	private static boolean setStart(AnimationState state, float start) {
		try {
			if (animStartTime == null) {
				animStartTime = AnimationState.class.getDeclaredField("animStartTime");
				animStartTime.setAccessible(true);
			}

			animStartTime.setFloat(state, start);
			return true;
		} catch (Throwable e) {
			unavailable = true;

			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.warn("Can't drive Essential's emote timing, so emotes won't follow the playhead exactly", e);
			}

			return false;
		}
	}
}
