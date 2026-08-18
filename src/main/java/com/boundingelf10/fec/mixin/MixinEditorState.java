package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.cosmetics.CosmeticsPipeline;
import com.boundingelf10.fec.emotes.EmoteTimeline;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import com.moulberry.flashback.state.EditorState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = EditorState.class, remap = false)
public class MixinEditorState {

	@Inject(method = "applyKeyframes(Lcom/moulberry/flashback/keyframe/handler/KeyframeHandler;FJ)V", at = @At("HEAD"))
	private void fec$applyEssentialKeyframes(KeyframeHandler keyframeHandler, float tick, long stamp, CallbackInfo ci) {
		if (keyframeHandler instanceof MinecraftKeyframeHandler) {
			EditorState editorState = (EditorState) (Object) this;
			CosmeticsPipeline.apply(editorState, tick, stamp);
			EmoteTimeline.apply(editorState, tick, stamp);
		}
	}
}
