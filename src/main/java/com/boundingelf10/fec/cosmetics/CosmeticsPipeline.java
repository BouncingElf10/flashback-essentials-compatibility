package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.boundingelf10.fec.state.EssentialState;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

public final class CosmeticsPipeline {

	private static final Map<UUID, Map<CosmeticSlot, CosmeticSpec>> APPLIED = new LinkedHashMap<>();

	private static ClientPacketListener lastConnection = null;
	private static int lastTick = -1;
	private static boolean warned = false;

	private CosmeticsPipeline() { }

	@Nullable
	public static CosmeticSpec applied(UUID player, CosmeticSlot slot) {
		return APPLIED.getOrDefault(player, Map.of()).get(slot);
	}

	public static void reset() {
		APPLIED.clear();
		lastTick = -1;
	}

	public static void refresh() {
		EditorState editorState = EditorStateManager.getCurrent();

		if (editorState != null && lastTick >= 0) {
			apply(editorState, lastTick, 0L);
		}
	}

	public static void apply(EditorState editorState, float tick, long stamp) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection != lastConnection) {
			lastConnection = connection;
			APPLIED.clear();
		}

		if (connection == null || !Flashback.isInReplay()) {
			return;
		}

		lastTick = (int) tick;

		try {
			IngameEquippedOutfitsManager manager = EssentialCosmetics.ingameManagerFor(connection);

			if (manager == null) {
				return;
			}

			Map<UUID, Map<CosmeticSlot, CosmeticSpec>> wanted = resolve(editorState, connection, (int) tick, stamp);

			if (wanted.isEmpty() && APPLIED.isEmpty()) {
				return;
			}

			push(manager, wanted);
			restore(manager, wanted);

			APPLIED.clear();
			APPLIED.putAll(wanted);
			warned = false;
		} catch (Exception e) {
			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to apply Essential cosmetic overrides", e);
			}
		}
	}

	private static Map<UUID, Map<CosmeticSlot, CosmeticSpec>> resolve(EditorState editorState, ClientPacketListener connection, int tick, long stamp) {
		Map<UUID, Map<CosmeticSlot, CosmeticSpec>> wanted = new LinkedHashMap<>();
		EssentialReplayState state = EssentialState.of(editorState);

		for (Map.Entry<String, Map<String, CosmeticSpec>> entry : state.overrides().entrySet()) {
			UUID player = parse(entry.getKey());

			if (player == null) {
				continue;
			}

			for (Map.Entry<String, CosmeticSpec> slotEntry : entry.getValue().entrySet()) {
				CosmeticSlot slot = CosmeticSlots.of(slotEntry.getKey());

				if (!CosmeticSlot.EMOTE.equals(slot)) {
					wanted.computeIfAbsent(player, ignored -> new LinkedHashMap<>()).put(slot, slotEntry.getValue());
				}
			}
		}

		for (Map.Entry<UUID, Map<CosmeticSlot, CosmeticTimeline.Decision>> entry
			: CosmeticTimeline.collect(editorState, tick, stamp).entrySet()) {

			for (Map.Entry<CosmeticSlot, CosmeticTimeline.Decision> slotEntry : entry.getValue().entrySet()) {
				CosmeticSlot slot = slotEntry.getKey();

				if (CosmeticSlot.EMOTE.equals(slot)) {
					continue;
				}

				CosmeticSpec spec = slotEntry.getValue().spec();

				if (spec == null) {
					Map<CosmeticSlot, CosmeticSpec> slots = wanted.get(entry.getKey());

					if (slots != null && slots.remove(slot) != null && slots.isEmpty()) {
						wanted.remove(entry.getKey());
					}
				} else {
					wanted.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>()).put(slot, spec);
				}
			}
		}

		for (UUID player : hiddenPlayers(state, connection)) {
			Map<CosmeticSlot, CosmeticSpec> slots = wanted.computeIfAbsent(player, ignored -> new LinkedHashMap<>());

			for (CosmeticSlot slot : CosmeticSlots.wearable()) {
				slots.put(slot, CosmeticSpec.nothingIn(slot));
			}
		}

		return wanted;
	}

	private static Set<UUID> hiddenPlayers(EssentialReplayState state, ClientPacketListener connection) {
		Set<UUID> hidden = new LinkedHashSet<>();

		if (state.hideAllCosmetics) {
			hidden.addAll(EssentialCosmetics.knownPlayers(connection, Minecraft.getInstance().level));
		} else {
			for (String key : state.hidden()) {
				UUID player = parse(key);

				if (player != null) {
					hidden.add(player);
				}
			}
		}

		if (state.hideCameraPlayerCosmetics) {
			Entity camera = Minecraft.getInstance().getCameraEntity();

			if (camera != null) {
				hidden.add(camera.getUUID());
			}
		}

		return hidden;
	}

	private static void push(IngameEquippedOutfitsManager manager, Map<UUID, Map<CosmeticSlot, CosmeticSpec>> wanted) {
		for (Map.Entry<UUID, Map<CosmeticSlot, CosmeticSpec>> entry : wanted.entrySet()) {
			Outfit outfit = EssentialCosmetics.outfitOf(manager, entry.getKey());
			List<Update> updates = new ArrayList<>();

			for (Map.Entry<CosmeticSlot, CosmeticSpec> slotEntry : entry.getValue().entrySet()) {
				EquippedCosmeticId equipped = slotEntry.getValue().equipped();

				if (!Objects.equals(outfit.getCosmetics().get(slotEntry.getKey()), equipped)) {
					updates.add(new Update.Cosmetic(slotEntry.getKey(), equipped));
				}
			}

			if (!updates.isEmpty()) {
				manager.applyUpdates(entry.getKey(), updates);
			}
		}
	}

	private static void restore(IngameEquippedOutfitsManager manager, Map<UUID, Map<CosmeticSlot, CosmeticSpec>> wanted) {
		for (Map.Entry<UUID, Map<CosmeticSlot, CosmeticSpec>> entry : APPLIED.entrySet()) {
			Map<CosmeticSlot, CosmeticSpec> stillWanted = wanted.getOrDefault(entry.getKey(), Map.of());
			List<Update> updates = new ArrayList<>();

			for (CosmeticSlot slot : entry.getValue().keySet()) {
				if (!stillWanted.containsKey(slot)) {
					updates.add(new Update.Cosmetic(slot, RecordedCosmetics.get(entry.getKey(), slot)));
				}
			}

			if (!updates.isEmpty()) {
				manager.applyUpdates(entry.getKey(), updates);
			}
		}
	}

	@Nullable
	private static UUID parse(String uuid) {
		try {
			return UUID.fromString(uuid);
		} catch (IllegalArgumentException notAUuid) {
			return null;
		}
	}
}
