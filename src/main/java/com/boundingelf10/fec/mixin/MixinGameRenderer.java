package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.emotes.EmoteTimeline;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Inject(method = "extract", at = @At("HEAD"))
	private void fec$holdEssentialEmotes(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		EmoteTimeline.hold(deltaTracker);
	}
}
