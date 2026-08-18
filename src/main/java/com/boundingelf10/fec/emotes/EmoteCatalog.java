package com.boundingelf10.fec.emotes;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.cosmetics.CosmeticCatalog;

import gg.essential.cosmetics.events.AnimationEvent;
import gg.essential.cosmetics.events.AnimationEventType;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.model.BedrockModel;
import gg.essential.network.connectionmanager.cosmetics.AssetLoader;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.util.GuiEssentialPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public final class EmoteCatalog {

	public record Info(boolean loops, int lengthTicks, List<String> animationVariants) { }

    private static final Map<String, Info> CACHE = new ConcurrentHashMap<>();

	private static boolean warned = false;

	private EmoteCatalog() { }

	public static List<Cosmetic> emotes() {
		return CosmeticCatalog.forSlot(CosmeticSlot.EMOTE);
	}

	@Nullable
	public static Cosmetic find(@Nullable String emoteId) {
		if (emoteId == null) {
			return null;
		}

		for (Cosmetic cosmetic : emotes()) {
			if (cosmetic.getId().equals(emoteId)) {
				return cosmetic;
			}
		}

		return null;
	}

	public static String displayNameOf(@Nullable String emoteId) {
		Cosmetic cosmetic = find(emoteId);

		if (cosmetic != null) {
			return CosmeticCatalog.displayNameOf(cosmetic);
		}

		return emoteId == null ? "No emote" : emoteId;
	}

	@Nullable
	public static Info infoOf(@Nullable String emoteId, @Nullable String variant, @Nullable String animationVariant) {
		if (emoteId == null) {
			return null;
		}

		String key = emoteId + '\u0000' + variant + '\u0000' + animationVariant;
		Info cached = CACHE.get(key);

		if (cached != null) {
			return cached;
		}

		Info info = readInfo(emoteId, variant, animationVariant);

		if (info != null) {
			CACHE.put(key, info);
		}

		return info;
	}

	@Nullable
	private static Info readInfo(String emoteId, @Nullable String variant, @Nullable String animationVariant) {
		Cosmetic cosmetic = find(emoteId);

		if (cosmetic == null) {
			return null;
		}

		BedrockModel model = modelOf(cosmetic, variant);

		if (model == null) {
			return null;
		}

		List<AnimationEvent> events = new ArrayList<>();
		List<String> names = new ArrayList<>();

		for (AnimationEvent event : model.getAnimationEvents()) {
			if (event.getType() == AnimationEventType.EMOTE) {
				events.add(event);
				names.add(event.getName());
			}
		}

		AnimationEvent chosen = null;

		if (animationVariant != null) {
			for (AnimationEvent event : events) {
				if (event.getName().equals(animationVariant)) {
					chosen = event;
					break;
				}
			}
		}

		boolean loops = events.isEmpty();
		float seconds = 0.0F;

		for (AnimationEvent event : chosen == null ? events : List.of(chosen)) {
			loops |= event.getLoops() == 0;
			seconds = Math.max(seconds, event.getTotalTime(model, 0.0F));
		}

		return new Info(loops, Math.max(1, Math.round(seconds * 20.0F)), List.copyOf(names));
	}

	@Nullable
	private static BedrockModel modelOf(Cosmetic cosmetic, @Nullable String variant) {
		try {
			String name = variant != null ? variant : cosmetic.getDefaultVariantName();

			if (name == null) {
				return null;
			}

			CompletableFuture<BedrockModel> model = GuiEssentialPlatform.Companion.getPlatform()
				.getModelLoader()
				.getModel(cosmetic, name, AssetLoader.Priority.Low);

			return model.isDone() && !model.isCompletedExceptionally() ? model.join() : null;
		} catch (Exception e) {
			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to read an Essential emote's model", e);
			}

			return null;
		}
	}
}
