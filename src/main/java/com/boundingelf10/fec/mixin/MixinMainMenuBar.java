package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ui.EssentialWindow;
import com.moulberry.flashback.editor.ui.windows.MainMenuBar;

import imgui.moulberry90.ImGui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MainMenuBar.class, remap = false)
public class MixinMainMenuBar {

	@Inject(method = "renderInner", at = @At("TAIL"))
	private static void fec$addEssentialMenuItem(CallbackInfo ci) {
		if (ImGui.menuItem("Essential##FecEssential")) {
			EssentialWindow.toggle();
		}
	}
}
