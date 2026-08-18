package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ext.EssentialCosmeticsMeta;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moulberry.flashback.record.FlashbackMeta;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = FlashbackMeta.class, remap = false)
public class MixinFlashbackMeta implements EssentialCosmeticsMeta {

	@Unique private static final String FEC$KEY = "essential_cosmetics";
	@Unique private boolean fec$hasCosmetics = false;
	@Unique private int fec$version = 0;

	@Override
	public boolean fec$hasEssentialCosmetics() {
		return this.fec$hasCosmetics;
	}

	@Override
	public int fec$essentialCosmeticsVersion() {
		return this.fec$version;
	}

	@Override
	public void fec$setEssentialCosmetics(int version) {
		this.fec$hasCosmetics = true;
		this.fec$version = version;
	}

	@Inject(method = "toJson", at = @At("RETURN"))
	private void fec$writeCosmeticsFlag(CallbackInfoReturnable<JsonObject> cir) {
		if (this.fec$hasCosmetics) {
			cir.getReturnValue().addProperty(FEC$KEY, this.fec$version);
		}
	}

	@Inject(method = "fromJson", at = @At("RETURN"))
	private static void fec$readCosmeticsFlag(JsonObject meta, CallbackInfoReturnable<FlashbackMeta> cir) {
		FlashbackMeta parsed = cir.getReturnValue();

		if (parsed == null || !meta.has(FEC$KEY)) {
			return;
		}

		JsonElement value = meta.get(FEC$KEY);

		boolean versioned = value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
		((EssentialCosmeticsMeta) parsed).fec$setEssentialCosmetics(versioned ? value.getAsInt() : 0);
	}
}
