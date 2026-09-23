package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.cosmetics.CosmeticsPipeline;
import com.boundingelf10.fec.emotes.EmoteTimeline;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import com.moulberry.flashback.state.EditorState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = EditorState.class, remap = false)
public class MixinEditorState {

	@Unique
	private static final boolean fec$hasStampedApply = fec$hasStampedApply();

	@Inject(method = "applyKeyframes(Lcom/moulberry/flashback/keyframe/handler/KeyframeHandler;FJ)V", at = @At("HEAD"), require = 0)
	private void fec$applyEssentialKeyframes(KeyframeHandler keyframeHandler, float tick, long stamp, CallbackInfo ci) {
		fec$apply(keyframeHandler, tick, stamp);
	}

	@Inject(method = "applyKeyframes(Lcom/moulberry/flashback/keyframe/handler/KeyframeHandler;F)V", at = @At("HEAD"), require = 0)
	private void fec$applyEssentialKeyframesLegacy(KeyframeHandler keyframeHandler, float tick, CallbackInfo ci) {
		if (!fec$hasStampedApply) {
			fec$apply(keyframeHandler, tick, 0L);
		}
	}

	@Unique
	private void fec$apply(KeyframeHandler keyframeHandler, float tick, long stamp) {
		if (keyframeHandler instanceof MinecraftKeyframeHandler) {
			EditorState editorState = (EditorState) (Object) this;
			CosmeticsPipeline.apply(editorState, tick, stamp);
			EmoteTimeline.apply(editorState, tick, stamp);
		}
	}

	@Unique
	private static boolean fec$hasStampedApply() {
		try {
			EditorState.class.getMethod("applyKeyframes", KeyframeHandler.class, float.class, long.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
