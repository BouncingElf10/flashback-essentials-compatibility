package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.ui.TimelineBar;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.mod.cosmetics.settings.CosmeticSetting;

import imgui.moulberry90.ImDrawList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public class EmoteKeyframe extends Keyframe {

	public static final String TYPE_ID = "fec_essential_emote";

	public static final UUID NO_PLAYER = new UUID(0L, 0L);
	private static final int BODY_COLOUR = 0x00532B4D;

	public UUID player;
	@Nullable public String emoteId;
	@Nullable public String variant;
	@Nullable public String animationVariant;

	public int durationTicks;

	public float speed = 1.0F;
	public int freezeTicks = -1;

	private final TimelineBar bar = new TimelineBar();

	public EmoteKeyframe(UUID player, @Nullable String emoteId, @Nullable String variant, @Nullable String animationVariant, int durationTicks) {
		this.player = player;
		this.emoteId = emoteId;
		this.variant = variant;
		this.animationVariant = animationVariant;
		this.durationTicks = durationTicks;
		this.interpolationType(InterpolationType.HOLD);
	}

	public boolean isFrozen() {
		return this.freezeTicks >= 0;
	}

	public float resolvedSpeed() {
		return this.speed <= 0.0F ? 1.0F : this.speed;
	}

	public float animationTicksAt(float tick, int startTick) {
		return this.isFrozen() ? this.freezeTicks : Math.max(0.0F, tick - startTick) * this.resolvedSpeed();
	}

	@Override
	public KeyframeType<?> keyframeType() {
		return EmoteKeyframeType.INSTANCE;
	}

	@Override
	public Keyframe copy() {
		EmoteKeyframe copy = new EmoteKeyframe(this.player, this.emoteId, this.variant, this.animationVariant, this.durationTicks);
		copy.speed = this.speed;
		copy.freezeTicks = this.freezeTicks;
		return copy;
	}

	@Override
	public InterpolationType interpolationType() {
		return InterpolationType.HOLD;
	}

	@Override
	@Nullable
	public KeyframeChange createChange() {
		return null;
	}

	@Override
	@Nullable
	public KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount) {
		return null;
	}

	@Override
	@Nullable
	public KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float amount) {
		return null;
	}

	@Nullable
	public EquippedCosmeticId equipped() {
		if (this.emoteId == null) {
			return null;
		}

		List<CosmeticSetting> settings = new ArrayList<>(2);

		if (this.variant != null) {
			settings.add(new CosmeticSetting.Variant(null, true, new CosmeticSetting.Variant.Data(this.variant)));
		}

		if (this.animationVariant != null) {
			settings.add(new CosmeticSetting.AnimationVariant(null, true,
				new CosmeticSetting.AnimationVariant.Data(this.animationVariant)));
		}

		return new EquippedCosmeticId(this.emoteId, List.copyOf(settings));
	}

	public int resolvedDurationTicks() {
		if (this.durationTicks > 0) {
			return this.durationTicks;
		}

		if (this.emoteId == null || this.isFrozen()) {
			return -1;
		}

		EmoteCatalog.Info info = EmoteCatalog.infoOf(this.emoteId, this.variant, this.animationVariant);

		if (info == null || info.loops()) {
			return -1;
		}

		return Math.max(1, Math.round(info.lengthTicks() / this.resolvedSpeed()));
	}

	public String label() {
		return EmoteCatalog.displayNameOf(this.emoteId);
	}

	@Override
	public float getCustomWidthInTicks() {
		return this.bar.widthInTicks(this.resolvedDurationTicks());
	}

	@Override
	public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
		EmoteKeyframeUi.renderFields(this, "Edit", new EmoteKeyframeUi.Sink() {
			@Override
			public void player(UUID player) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).player = player);
			}

			@Override
			public void emote(@Nullable String emoteId) {
				update.accept(keyframe -> {
					EmoteKeyframe emote = (EmoteKeyframe) keyframe;
					emote.emoteId = emoteId;
					emote.variant = null;
					emote.animationVariant = null;
				});
			}

			@Override
			public void variant(@Nullable String variant) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).variant = variant);
			}

			@Override
			public void animationVariant(@Nullable String animationVariant) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).animationVariant = animationVariant);
			}

			@Override
			public void duration(int ticks) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).durationTicks = ticks);
			}

			@Override
			public void speed(float speed) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).speed = speed);
			}

			@Override
			public void freeze(int ticks) {
				update.accept(keyframe -> ((EmoteKeyframe) keyframe).freezeTicks = ticks);
			}
		});
	}

	@Override
	public void drawOnTimeline(ImDrawList drawList, int keyframeSize, float x, float y, int colour, float timelineScale, float minTimelineX, float maxTimelineX, int tick, TreeMap<Integer, Keyframe> keyframeTimes) {
		this.bar.draw(drawList, keyframeSize, x, y, colour, timelineScale, minTimelineX, maxTimelineX, tick,
			keyframeTimes, this.resolvedDurationTicks(), BODY_COLOUR, this.label());
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", TYPE_ID);
		json.addProperty("player", this.player.toString());

		if (this.emoteId != null) {
			json.addProperty("emote", this.emoteId);
		}

		if (this.variant != null) {
			json.addProperty("variant", this.variant);
		}

		if (this.animationVariant != null) {
			json.addProperty("animation", this.animationVariant);
		}

		json.addProperty("duration", this.durationTicks);

		if (this.speed != 1.0F) {
			json.addProperty("speed", this.speed);
		}

		if (this.isFrozen()) {
			json.addProperty("freeze", this.freezeTicks);
		}

		return json;
	}

	public static EmoteKeyframe fromJson(JsonObject json) {
		EmoteKeyframe keyframe = new EmoteKeyframe(
			player(json),
			string(json, "emote"),
			string(json, "variant"),
			string(json, "animation"),
			integer(json, "duration", 0)
		);

		keyframe.speed = number(json, "speed", 1.0F);
		keyframe.freezeTicks = integer(json, "freeze", -1);
		return keyframe;
	}

	public static boolean isUntagged(JsonObject json) {
		return json.has("emoteId") || json.has("durationTicks") || json.has("freezeTicks");
	}

	public static EmoteKeyframe fromUntaggedJson(JsonObject json) {
		EmoteKeyframe keyframe = new EmoteKeyframe(
			player(json),
			string(json, "emoteId"),
			string(json, "variant"),
			string(json, "animationVariant"),
			integer(json, "durationTicks", 0)
		);

		keyframe.speed = number(json, "speed", 1.0F);
		keyframe.freezeTicks = integer(json, "freezeTicks", -1);
		return keyframe;
	}

	private static UUID player(JsonObject json) {
		try {
			return UUID.fromString(json.get("player").getAsString());
		} catch (Exception notAUuid) {
			return NO_PLAYER;
		}
	}

	@Nullable
	private static String string(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
	}

	private static int integer(JsonObject json, String key, int fallback) {
		try {
			JsonElement value = json.get(key);
			return value == null || !value.isJsonPrimitive() ? fallback : value.getAsInt();
		} catch (Exception notANumber) {
			return fallback;
		}
	}

	private static float number(JsonObject json, String key, float fallback) {
		try {
			JsonElement value = json.get(key);
			return value == null || !value.isJsonPrimitive() ? fallback : value.getAsFloat();
		} catch (Exception notANumber) {
			return fallback;
		}
	}
}
