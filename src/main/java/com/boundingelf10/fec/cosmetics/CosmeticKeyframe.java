package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.boundingelf10.fec.ui.CosmeticKeyframeUi;
import com.boundingelf10.fec.ui.TimelineBar;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;

import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;

import imgui.moulberry90.ImDrawList;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;


public class CosmeticKeyframe extends Keyframe {
	public static final String TYPE_ID = "fec_essential_cosmetic";
	private static final int BODY_COLOUR = 0x002B4D53;

	public UUID player;
	public String slot;
	public boolean fromReplay;
	public CosmeticSpec cosmetic;

	public int durationTicks;

	private final TimelineBar bar = new TimelineBar();

	public CosmeticKeyframe(UUID player, String slot, boolean fromReplay, CosmeticSpec cosmetic) {
		this.player = player;
		this.slot = slot;
		this.fromReplay = fromReplay;
		this.cosmetic = cosmetic;
		this.interpolationType(InterpolationType.HOLD);
	}

	public CosmeticSlot slot() {
		return CosmeticSlots.of(this.slot);
	}

	public long endTick(int startTick, @Nullable Integer nextTick) {
		long end = nextTick == null ? Long.MAX_VALUE : nextTick;
		return this.durationTicks > 0 ? Math.min(end, (long) startTick + this.durationTicks) : end;
	}

	@Override
	public KeyframeType<?> keyframeType() {
		return CosmeticKeyframeType.INSTANCE;
	}

	@Override
	public Keyframe copy() {
		CosmeticKeyframe copy = new CosmeticKeyframe(this.player, this.slot, this.fromReplay, this.cosmetic.copy());
		copy.durationTicks = this.durationTicks;
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

	public String label() {
		if (this.fromReplay) {
			return CosmeticSlots.displayNameOf(this.slot()) + ": from replay";
		}

		if (this.cosmetic.isNothing()) {
			return CosmeticSlots.displayNameOf(this.slot()) + ": none";
		}

		Cosmetic found = CosmeticCatalog.find(this.slot(), this.cosmetic.id);
		return CosmeticSlots.displayNameOf(this.slot()) + ": "
			+ (found == null ? this.cosmetic.id : CosmeticCatalog.displayNameOf(found));
	}

	@Override
	public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
		CosmeticKeyframeUi.renderFields(this, "Edit", new CosmeticKeyframeUi.Sink() {
			@Override
			public void player(UUID player) {
				update.accept(keyframe -> ((CosmeticKeyframe) keyframe).player = player);
			}

			@Override
			public void slot(String slot) {
				update.accept(keyframe -> {
					CosmeticKeyframe cosmeticKeyframe = (CosmeticKeyframe) keyframe;
					cosmeticKeyframe.slot = slot;
					cosmeticKeyframe.cosmetic = CosmeticSpec.nothing();
					cosmeticKeyframe.fromReplay = true;
				});
			}

			@Override
			public void cosmetic(boolean fromReplay, CosmeticSpec spec) {
				update.accept(keyframe -> {
					CosmeticKeyframe cosmeticKeyframe = (CosmeticKeyframe) keyframe;
					cosmeticKeyframe.fromReplay = fromReplay;
					cosmeticKeyframe.cosmetic = spec;
				});
			}

			@Override
			public void duration(int ticks) {
				update.accept(keyframe -> ((CosmeticKeyframe) keyframe).durationTicks = ticks);
			}
		});
	}

	@Override
	public float getCustomWidthInTicks() {
		return this.bar.widthInTicks(this.durationTicks);
	}

	@Override
	public void drawOnTimeline(ImDrawList drawList, int keyframeSize, float x, float y, int colour, float timelineScale, float minTimelineX, float maxTimelineX, int tick, TreeMap<Integer, Keyframe> keyframeTimes) {
		this.bar.draw(drawList, keyframeSize, x, y, colour, timelineScale, minTimelineX, maxTimelineX, tick,
			keyframeTimes, this.durationTicks, BODY_COLOUR, this.label());
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", TYPE_ID);
		json.addProperty("player", this.player.toString());
		json.addProperty("slot", this.slot);
		json.addProperty("fromReplay", this.fromReplay);

		if (this.cosmetic.id != null) {
			json.addProperty("cosmetic", this.cosmetic.id);
		}

		if (this.cosmetic.variant != null) {
			json.addProperty("variant", this.cosmetic.variant);
		}

		if (this.cosmetic.animationVariant != null) {
			json.addProperty("animation", this.cosmetic.animationVariant);
		}

		if (this.cosmetic.side != null) {
			json.addProperty("side", this.cosmetic.side);
		}

		if (this.cosmetic.x != null) {
			json.addProperty("x", this.cosmetic.x);
		}

		if (this.cosmetic.y != null) {
			json.addProperty("y", this.cosmetic.y);
		}

		if (this.cosmetic.z != null) {
			json.addProperty("z", this.cosmetic.z);
		}

		if (this.durationTicks > 0) {
			json.addProperty("duration", this.durationTicks);
		}

		return json;
	}

	public static CosmeticKeyframe fromJson(JsonObject json) {
		CosmeticSpec spec = new CosmeticSpec();
		spec.id = string(json, "cosmetic");
		spec.variant = string(json, "variant");
		spec.animationVariant = string(json, "animation");
		spec.side = string(json, "side");
		spec.x = number(json, "x");
		spec.y = number(json, "y");
		spec.z = number(json, "z");

		return keyframe(json, spec, integer(json, "duration"));
	}

	public static boolean isUntagged(JsonObject json) {
		JsonElement cosmetic = json.get("cosmetic");
		return json.has("fromReplay") || (cosmetic != null && cosmetic.isJsonObject());
	}

	public static CosmeticKeyframe fromUntaggedJson(JsonObject json) {
		JsonElement nested = json.get("cosmetic");
		JsonObject cosmetic = nested != null && nested.isJsonObject() ? nested.getAsJsonObject() : new JsonObject();

		CosmeticSpec spec = new CosmeticSpec();
		spec.id = string(cosmetic, "id");
		spec.variant = string(cosmetic, "variant");
		spec.animationVariant = string(cosmetic, "animationVariant");
		spec.side = string(cosmetic, "side");
		spec.x = number(cosmetic, "x");
		spec.y = number(cosmetic, "y");
		spec.z = number(cosmetic, "z");

		return keyframe(json, spec, integer(json, "durationTicks"));
	}

	private static CosmeticKeyframe keyframe(JsonObject json, CosmeticSpec spec, int durationTicks) {
		UUID player;

		try {
			player = UUID.fromString(json.get("player").getAsString());
		} catch (Exception notAUuid) {
			player = EmoteKeyframe.NO_PLAYER;
		}

		String slot = string(json, "slot");
		JsonElement fromReplay = json.get("fromReplay");

		CosmeticKeyframe keyframe = new CosmeticKeyframe(
			player,
			slot == null ? CosmeticSlot.HAT.getId() : slot,
			fromReplay != null && fromReplay.isJsonPrimitive() && fromReplay.getAsBoolean(),
			spec);

		keyframe.durationTicks = Math.max(0, durationTicks);
		return keyframe;
	}

	@Nullable
	private static String string(JsonObject json, String key) {
		JsonElement value = json.get(key);
		return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
	}

	private static int integer(JsonObject json, String key) {
		try {
			JsonElement value = json.get(key);
			return value == null || !value.isJsonPrimitive() ? 0 : value.getAsInt();
		} catch (Exception notANumber) {
			return 0;
		}
	}

	@Nullable
	private static Float number(JsonObject json, String key) {
		try {
			JsonElement value = json.get(key);
			return value == null || !value.isJsonPrimitive() ? null : value.getAsFloat();
		} catch (Exception notANumber) {
			return null;
		}
	}
}
