package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.state.EssentialReplayState;
import com.boundingelf10.fec.state.EssentialState;
import com.moulberry.flashback.Flashback;

import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.mod.cosmetics.CosmeticSlot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import org.jetbrains.annotations.Nullable;

public final class CosmeticsOverrides {
	private static ClientPacketListener lastConnection = null;

	private CosmeticsOverrides() { }

	public static boolean hasAny(UUID player) {
		return EssentialState.currentOrEmpty().hasOverrides(player);
	}

	public static Map<CosmeticSlot, CosmeticSpec> of(UUID player) {
		Map<CosmeticSlot, CosmeticSpec> slots = new LinkedHashMap<>();

		for (Map.Entry<String, CosmeticSpec> entry : EssentialState.currentOrEmpty().overridesOf(player).entrySet()) {
			slots.put(CosmeticSlots.of(entry.getKey()), entry.getValue());
		}

		return slots;
	}

	@Nullable
	public static CosmeticSpec get(UUID player, CosmeticSlot slot) {
		EssentialReplayState state = EssentialState.current();
		return state == null ? null : state.override(player, slot);
	}

	public static void set(UUID player, CosmeticSlot slot, @Nullable CosmeticSpec spec) {
		EssentialState.edit(state -> state.setOverride(player, slot, spec));
		CosmeticsPipeline.refresh();
	}

	public static void clear(UUID player) {
		EssentialState.edit(state -> state.clearOverrides(player));
		CosmeticsPipeline.refresh();
	}

	public static void tick() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection != lastConnection) {
			lastConnection = connection;
			CosmeticsPipeline.reset();
			RecordedCosmetics.clear();
			return;
		}

		if (!Flashback.isInReplay() || connection == null) {
			return;
		}

		IngameEquippedOutfitsManager manager = EssentialCosmetics.ingameManagerFor(connection);

		if (manager == null) {
			return;
		}

		EssentialCosmetics.bindOutfitManager(connection, manager);
		RecordedCosmetics.observe(manager, EssentialCosmetics.knownPlayers(connection, Minecraft.getInstance().level));
		CosmeticsPipeline.refresh();
	}
}
