package com.boundingelf10.fec.mixin;

import com.boundingelf10.fec.ext.EssentialStateHolder;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.moulberry.flashback.state.EditorState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EditorState.class, remap = false)
public class MixinEditorStateEssential implements EssentialStateHolder {

	private EssentialReplayState fecEssential = new EssentialReplayState();

	@Override
	@Unique
	public EssentialReplayState fec$essential() {
		if (this.fecEssential == null) {
			this.fecEssential = new EssentialReplayState();
		}

		return this.fecEssential;
	}
}
