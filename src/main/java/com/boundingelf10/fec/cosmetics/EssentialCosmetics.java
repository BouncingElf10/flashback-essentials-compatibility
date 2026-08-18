package com.boundingelf10.fec.cosmetics;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.EquippedOutfitsManagerMcKt;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update;
import gg.essential.cosmetics.OutfitUpdatesPayload;
import gg.essential.mixins.impl.client.network.NetworkPlayerInfoExt;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import kotlin.Pair;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;


public final class EssentialCosmetics {

	private EssentialCosmetics() { }

	public static Outfit empty() {
		return Outfit.Companion.getEMPTY();
	}

	@Nullable
	public static EquippedOutfitsManager managerFor(@Nullable ClientPacketListener connection) {
		if (connection == null) {
			return null;
		}

		return EquippedOutfitsManagerMcKt.getEquippedOutfitsManager(connection);
	}

	@Nullable
	public static IngameEquippedOutfitsManager ingameManagerFor(@Nullable ClientPacketListener connection) {
		return managerFor(connection) instanceof IngameEquippedOutfitsManager ingame ? ingame : null;
	}

	public static Outfit outfitOf(EquippedOutfitsManager manager, UUID player) {
		return manager.getEquippedCosmeticsState(player).getUntracked();
	}

	public static void bindOutfitManager(ClientPacketListener connection, EquippedOutfitsManager manager) {
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			if (info instanceof NetworkPlayerInfoExt ext && ext.getEssential$equippedOutfitsManager() != manager) {
				ext.setEssential$equippedOutfitsManager(manager);
			}
		}
	}

	public static Set<UUID> knownPlayers(ClientPacketListener connection, @Nullable ClientLevel level) {
		Set<UUID> players = new LinkedHashSet<>();

		for (PlayerInfo info : connection.getOnlinePlayers()) {
			players.add(info.getProfile().id());
		}

		if (level != null) {
			for (Player player : level.players()) {
				players.add(player.getUUID());
			}
		}

		return players;
	}

    public static Map<UUID, Outfit> currentOutfits(ClientPacketListener connection, @Nullable ClientLevel level) {
		EquippedOutfitsManager manager = managerFor(connection);

		if (manager == null) {
			return Map.of();
		}

		Map<UUID, Outfit> outfits = new LinkedHashMap<>();
		Outfit empty = empty();

		for (UUID player : knownPlayers(connection, level)) {
			Outfit outfit = outfitOf(manager, player);

			if (!empty.equals(outfit)) {
				outfits.put(player, outfit);
			}
		}

		return outfits;
	}

	public static List<Pair<UUID, List<Update>>> diff(Map<UUID, Outfit> previous, Map<UUID, Outfit> current) {
		List<Pair<UUID, List<Update>>> changes = new ArrayList<>();
		Outfit empty = empty();

		for (Map.Entry<UUID, Outfit> entry : current.entrySet()) {
			UUID player = entry.getKey();
			Outfit now = entry.getValue();
			Outfit before = previous.getOrDefault(player, empty);

			if (now.equals(before)) {
				continue;
			}

			List<Update> updates = new ArrayList<>();

			if (!previous.containsKey(player)) {
				updates.add(Update.Remove.INSTANCE);
			}

			updates.addAll(updatesBetween(before, now));
			changes.add(new Pair<>(player, updates));
		}

		for (UUID player : previous.keySet()) {
			if (!current.containsKey(player)) {
				changes.add(new Pair<>(player, List.of(Update.Remove.INSTANCE)));
			}
		}

		return changes;
	}

	private static List<Update> updatesBetween(Outfit before, Outfit after) {
		List<Update> updates = new ArrayList<>();

		for (Map.Entry<CosmeticSlot, EquippedCosmeticId> entry : after.getCosmetics().entrySet()) {
			CosmeticSlot slot = entry.getKey();

			if (!entry.getValue().equals(before.getCosmetics().get(slot))) {
				updates.add(new Update.Cosmetic(slot, entry.getValue()));
			}
		}

		for (CosmeticSlot slot : before.getCosmetics().keySet()) {
			if (!after.getCosmetics().containsKey(slot)) {
				updates.add(new Update.Cosmetic(slot, null));
			}
		}

		if (!Objects.equals(before.getSkin(), after.getSkin())) {
			updates.add(new Update.Skin(after.getSkin()));
		}

		return updates;
	}

	public static List<Pair<UUID, List<Update>>> absoluteState(Set<UUID> players, Map<UUID, Outfit> outfits) {
		List<Pair<UUID, List<Update>>> state = new ArrayList<>();
		Outfit empty = empty();

		for (UUID player : players) {
			Outfit outfit = outfits.getOrDefault(player, empty);
			List<Update> updates = new ArrayList<>();
			updates.add(Update.Remove.INSTANCE);

			for (Map.Entry<CosmeticSlot, EquippedCosmeticId> entry : outfit.getCosmetics().entrySet()) {
				updates.add(new Update.Cosmetic(entry.getKey(), entry.getValue()));
			}

			if (outfit.getSkin() != null) {
				updates.add(new Update.Skin(outfit.getSkin()));
			}

			state.add(new Pair<>(player, updates));
		}

		return state;
	}

	public static ClientboundCustomPayloadPacket packet(List<Pair<UUID, List<Update>>> updates) {
		return new ClientboundCustomPayloadPacket(new OutfitUpdatesPayload(updates));
	}

	public static boolean isOutfitUpdate(CustomPacketPayload payload) {
		return payload instanceof OutfitUpdatesPayload;
	}

	@Nullable
	public static String equippedIdIn(Outfit outfit, CosmeticSlot slot) {
		EquippedCosmeticId equipped = outfit.getCosmetics().get(slot);
		return equipped == null ? null : equipped.getId();
	}

}
