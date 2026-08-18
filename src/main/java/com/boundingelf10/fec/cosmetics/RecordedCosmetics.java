package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.emotes.EmoteTimeline;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public final class RecordedCosmetics {

	private static final Map<UUID, Map<CosmeticSlot, EquippedCosmeticId>> RECORDED = new LinkedHashMap<>();

	private RecordedCosmetics() { }

	public static void observe(IngameEquippedOutfitsManager manager, Set<UUID> players) {
		for (UUID player : players) {
			Outfit outfit = EssentialCosmetics.outfitOf(manager, player);
			Map<CosmeticSlot, EquippedCosmeticId> slots = RECORDED.computeIfAbsent(player, ignored -> new LinkedHashMap<>());

			for (Map.Entry<CosmeticSlot, EquippedCosmeticId> entry : outfit.getCosmetics().entrySet()) {
				if (isFromReplay(player, entry.getKey(), entry.getValue())) {
					slots.put(entry.getKey(), entry.getValue());
				}
			}

			for (CosmeticSlot slot : List.copyOf(slots.keySet())) {
				if (!outfit.getCosmetics().containsKey(slot) && isFromReplay(player, slot, null)) {
					slots.remove(slot);
				}
			}
		}
	}

	private static boolean isFromReplay(UUID player, CosmeticSlot slot, @Nullable EquippedCosmeticId equipped) {
		if (CosmeticSlot.EMOTE.equals(slot) && EmoteTimeline.controls(player)) {
			return !Objects.equals(EmoteTimeline.forced(player), equipped);
		}

		CosmeticSpec applied = CosmeticsPipeline.applied(player, slot);

		if (applied == null) {
			return true;
		}

		return !Objects.equals(applied.equipped(), equipped);
	}

	@Nullable
	public static EquippedCosmeticId get(UUID player, CosmeticSlot slot) {
		return RECORDED.getOrDefault(player, Map.of()).get(slot);
	}

	public static void clear() {
		RECORDED.clear();
	}
}
