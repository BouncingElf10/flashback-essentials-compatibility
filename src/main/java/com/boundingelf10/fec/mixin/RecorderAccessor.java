package com.boundingelf10.fec.mixin;

import com.moulberry.flashback.record.FlashbackMeta;
import com.moulberry.flashback.record.Recorder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = Recorder.class, remap = false)
public interface RecorderAccessor {

	@Accessor("metadata")
	FlashbackMeta fec$metadata();
}
