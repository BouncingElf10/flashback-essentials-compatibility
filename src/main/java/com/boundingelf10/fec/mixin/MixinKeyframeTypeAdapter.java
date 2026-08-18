package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.cosmetics.CosmeticKeyframe;
import com.boundingelf10.fec.cosmetics.CosmeticSpec;
import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.moulberry.flashback.keyframe.Keyframe;

import gg.essential.mod.cosmetics.CosmeticSlot;

import java.lang.reflect.Type;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.moulberry.flashback.keyframe.Keyframe$TypeAdapter", remap = false)
public class MixinKeyframeTypeAdapter {

	@Inject(
		method = "serialize(Lcom/moulberry/flashback/keyframe/Keyframe;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void fec$serializeEmote(Keyframe src, Type type, JsonSerializationContext context, CallbackInfoReturnable<JsonElement> cir) {
		if (src instanceof EmoteKeyframe emote) {
			cir.setReturnValue(emote.toJson());
		} else if (src instanceof CosmeticKeyframe cosmetic) {
			cir.setReturnValue(cosmetic.toJson());
		}
	}

	@Inject(
		method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lcom/moulberry/flashback/keyframe/Keyframe;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void fec$deserializeEmote(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<Keyframe> cir) {
		if (!json.isJsonObject()) {
			return;
		}

		JsonObject object = json.getAsJsonObject();
		JsonElement id = object.get("type");

		if (id != null && id.isJsonPrimitive()) {
			if (EmoteKeyframe.TYPE_ID.equals(id.getAsString())) {
				cir.setReturnValue(read(object, false));
			} else if (CosmeticKeyframe.TYPE_ID.equals(id.getAsString())) {
				cir.setReturnValue(read(object, true));
			}

			return;
		}

		if (EmoteKeyframe.isUntagged(object)) {
			cir.setReturnValue(readUntagged(object, false));
		} else if (CosmeticKeyframe.isUntagged(object)) {
			cir.setReturnValue(readUntagged(object, true));
		}
	}

	private static Keyframe read(JsonObject object, boolean cosmetic) {
		try {
			return cosmetic ? CosmeticKeyframe.fromJson(object) : EmoteKeyframe.fromJson(object);
		} catch (Exception malformed) {
			return placeholder(cosmetic, malformed, object);
		}
	}

	private static Keyframe readUntagged(JsonObject object, boolean cosmetic) {
		try {
			return cosmetic ? CosmeticKeyframe.fromUntaggedJson(object) : EmoteKeyframe.fromUntaggedJson(object);
		} catch (Exception malformed) {
			return placeholder(cosmetic, malformed, object);
		}
	}

	private static Keyframe placeholder(boolean cosmetic, Exception cause, JsonObject object) {
		FlashbackEssentialsCompatibilityClient.LOGGER.warn(
			"Dropping an unreadable Essential keyframe: {}", object, cause);

		return cosmetic
			? new CosmeticKeyframe(EmoteKeyframe.NO_PLAYER, CosmeticSlot.HAT.getId(), true, CosmeticSpec.nothing())
			: new EmoteKeyframe(EmoteKeyframe.NO_PLAYER, null, null, null, 0);
	}
}
