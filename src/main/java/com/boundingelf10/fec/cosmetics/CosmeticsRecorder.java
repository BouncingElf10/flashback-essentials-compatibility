package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.ext.EssentialCosmeticsMeta;
import com.boundingelf10.fec.mixin.RecorderAccessor;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.record.Recorder;

import gg.essential.cosmetics.IngameEquippedOutfitsManager.Update;
import gg.essential.network.connectionmanager.cosmetics.EquippedOutfitsManager.Outfit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import kotlin.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

public final class CosmeticsRecorder {

	private static Map<UUID, Outfit> lastWritten = Map.of();
	private static ClientPacketListener lastConnection = null;

	private CosmeticsRecorder() { }

	public static void reset() {
		lastWritten = Map.of();
		lastConnection = null;
	}

	public static void tick() {
		Recorder recorder = Flashback.RECORDER;

		if (recorder == null || !recorder.readyToWrite()) {
			reset();
			return;
		}

		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection == null) {
			reset();
			return;
		}

		if (connection != lastConnection) {
			lastWritten = Map.of();
			lastConnection = connection;
		}

		try {
			Map<UUID, Outfit> current = EssentialCosmetics.currentOutfits(connection, Minecraft.getInstance().level);
			List<Pair<UUID, List<Update>>> updates = EssentialCosmetics.diff(lastWritten, current);

			if (!updates.isEmpty()) {
				recorder.writePacketAsync(EssentialCosmetics.packet(updates), ConnectionProtocol.PLAY);
			}

			markMetadata(recorder, current);
			lastWritten = current;
		} catch (Exception e) {
            FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to record Essential cosmetics", e);
			reset();
		}
	}

	public static void writeSnapshot(Recorder recorder, Consumer<Packet<? super ClientGamePacketListener>> consumer) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection == null) {
			return;
		}

		try {
			Set<UUID> players = EssentialCosmetics.knownPlayers(connection, Minecraft.getInstance().level);
			Map<UUID, Outfit> current = EssentialCosmetics.currentOutfits(connection, Minecraft.getInstance().level);
			List<Pair<UUID, List<Update>>> updates = EssentialCosmetics.absoluteState(players, current);

			if (!updates.isEmpty()) {
				consumer.accept(EssentialCosmetics.packet(updates));
			}

			markMetadata(recorder, current);

			lastWritten = current;
			lastConnection = connection;
		} catch (Exception e) {
			FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to snapshot Essential cosmetics", e);
		}
	}

	private static void markMetadata(Recorder recorder, Map<UUID, Outfit> outfits) {
		if (outfits.isEmpty()) {
			return;
		}

		((EssentialCosmeticsMeta) ((RecorderAccessor) recorder).fec$metadata()).fec$setEssentialCosmetics(EssentialCosmeticsMeta.CURRENT_VERSION);
	}
}
