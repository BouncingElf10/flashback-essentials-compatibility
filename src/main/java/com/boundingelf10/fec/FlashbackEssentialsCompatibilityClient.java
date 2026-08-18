package com.boundingelf10.fec;

import com.boundingelf10.fec.cosmetics.CosmeticKeyframeType;
import com.boundingelf10.fec.cosmetics.CosmeticsOverrides;
import com.boundingelf10.fec.cosmetics.CosmeticsRecorder;
import com.boundingelf10.fec.emotes.EmoteKeyframeType;
import com.boundingelf10.fec.emotes.EmoteTimeline;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.moulberry.flashback.keyframe.KeyframeRegistry;
import com.moulberry.flashback.state.EditorState;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlashbackEssentialsCompatibilityClient implements ClientModInitializer {
    public static final String MOD_ID = "flashback-essentials-compatibility";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		KeyframeRegistry.register(EmoteKeyframeType.INSTANCE);
		KeyframeRegistry.register(CosmeticKeyframeType.INSTANCE);
		checkEditorStateIsExtended();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
			CosmeticsRecorder.tick();
			CosmeticsOverrides.tick();
			EmoteTimeline.tick();
		});
	}

	private static void checkEditorStateIsExtended() {
		for (Field field : EditorState.class.getDeclaredFields()) {
			if (field.getType() == EssentialReplayState.class
				&& !Modifier.isStatic(field.getModifiers())
				&& !Modifier.isTransient(field.getModifiers())) {
				return;
			}
		}

		LOGGER.warn("Flashback's editor state didn't pick up this mod's field, so Essential cosmetic overrides won't be saved with the replay");
	}
}
