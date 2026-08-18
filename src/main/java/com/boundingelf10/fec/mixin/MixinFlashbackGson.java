package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.serialization.KeyframeAdapters;
import com.google.gson.GsonBuilder;
import com.moulberry.flashback.FlashbackGson;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FlashbackGson.class, remap = false)
public class MixinFlashbackGson {
	@Inject(method = "build()Lcom/google/gson/GsonBuilder;", at = @At("RETURN"))
	private static void fec$registerKeyframeAdapters(CallbackInfoReturnable<GsonBuilder> cir) {
		cir.getReturnValue().registerTypeAdapterFactory(new KeyframeAdapters());
	}
}
