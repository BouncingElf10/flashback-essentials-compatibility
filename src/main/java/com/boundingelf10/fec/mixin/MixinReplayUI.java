package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.cosmetics.CosmeticKeyframeType;
import com.boundingelf10.fec.emotes.EmoteKeyframeType;
import com.moulberry.flashback.editor.ui.ReplayUI;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ReplayUI.class, remap = false)
public class MixinReplayUI {

	@Inject(method = "buildMaterialIconRanges", at = @At("RETURN"), cancellable = true, require = 0)
	private static void fec$includeEmoteIcon(CallbackInfoReturnable<short[]> cir) {
		short[] ranges = cir.getReturnValue();

		if (ranges == null) {
			return;
		}

		int end = 0;

		while (end < ranges.length && ranges[end] != 0) {
			end++;
		}

		short[] extended = new short[end + 5];
		System.arraycopy(ranges, 0, extended, 0, end);
		extended[end] = (short) EmoteKeyframeType.ICON;
		extended[end + 1] = (short) EmoteKeyframeType.ICON;
		extended[end + 2] = (short) CosmeticKeyframeType.ICON;
		extended[end + 3] = (short) CosmeticKeyframeType.ICON;
		extended[end + 4] = 0;
		cir.setReturnValue(extended);
	}
}
