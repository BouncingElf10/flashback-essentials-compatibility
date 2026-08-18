package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ui.EssentialWindow;
import com.moulberry.flashback.editor.ui.windows.WindowType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WindowType.class, remap = false)
public class MixinWindowType {

	@Inject(method = "renderAll", at = @At("TAIL"))
	private static void fec$renderEssentialWindow(CallbackInfo ci) {
		EssentialWindow.render();
	}
}
