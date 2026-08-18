package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ui.CosmeticsOverrideUi;
import com.moulberry.flashback.editor.ui.windows.SelectedEntityPopup;
import com.moulberry.flashback.state.EditorState;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SelectedEntityPopup.class, remap = false)
public class MixinSelectedEntityPopup {

	@Inject(method = "render", at = @At("TAIL"))
	private static void fec$renderCosmeticsOverride(Entity entity, EditorState editorState, CallbackInfo ci) {
		CosmeticsOverrideUi.render(entity);
	}
}
