package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ext.EssentialCosmeticsMeta;
import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.screen.ReplaySummary;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = ReplaySummary.class, remap = false)
public class MixinReplaySummary {

	@Unique private static final String FEC$TAG = "Essential Cosmetics";
	@Unique private static final String FEC$OUTDATED = " (old format)";
    @Shadow @Final private FlashbackMeta metadata;

	@Unique private boolean fec$tagged = false;

	@Inject(method = "getInfo", at = @At("RETURN"))
	private void fec$appendCosmeticsTag(CallbackInfoReturnable<Component> cir) {
		if (this.fec$tagged) {
			return;
		}

		this.fec$tagged = true;
		EssentialCosmeticsMeta cosmetics = (EssentialCosmeticsMeta) (Object) this.metadata;

		if (!cosmetics.fec$hasEssentialCosmetics()) {
			return;
		}

		if (cir.getReturnValue() instanceof MutableComponent info) {
			if (this.metadata.totalTicks > 0) {
				info.append(" · ");
			}

			info.append(FEC$TAG);

			if (cosmetics.fec$essentialCosmeticsVersion() < EssentialCosmeticsMeta.CURRENT_VERSION) {
				info.append(FEC$OUTDATED);
			}
		}
	}
}
