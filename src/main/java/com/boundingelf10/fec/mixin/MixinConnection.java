package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.cosmetics.EssentialCosmetics;
import com.moulberry.flashback.Flashback;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {

	@Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
	private static void fec$dropLiveCosmeticsInReplay(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
		if (Flashback.isInReplay()
			&& packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)
			&& EssentialCosmetics.isOutfitUpdate(payload)) {
			ci.cancel();
		}
	}
}
