package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.cosmetics.CosmeticsRecorder;
import com.moulberry.flashback.record.Recorder;

import java.util.function.Consumer;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Recorder.class, remap = false)
public class MixinRecorder {

	@Inject(method = "writeCustomSnapshot", at = @At("HEAD"))
	private void fec$writeCosmeticsSnapshot(Consumer<Packet<? super ClientGamePacketListener>> consumer, CallbackInfo ci) {
		CosmeticsRecorder.writeSnapshot((Recorder) (Object) this, consumer);
	}
}
