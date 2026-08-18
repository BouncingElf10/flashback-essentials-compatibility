package com.boundingelf10.fec.config;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.cosmetics.CosmeticSpec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gg.essential.mod.cosmetics.CosmeticSlot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

import org.jetbrains.annotations.Nullable;

public final class EssentialPresets {

	public static final class Preset {
		public String name = "";
		public Map<String, CosmeticSpec> slots = new LinkedHashMap<>();

		public Map<String, CosmeticSpec> slots() {
			if (this.slots == null) {
				this.slots = new LinkedHashMap<>();
			}

			return this.slots;
		}
	}

	private static final class Data {
		List<Preset> presets = new ArrayList<>();
		Map<String, List<String>> recent = new LinkedHashMap<>();
	}

	private static final int MAX_RECENT = 8;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Data data = null;
	private static boolean warned = false;

	private EssentialPresets() { }

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir()
			.resolve(FlashbackEssentialsCompatibilityClient.MOD_ID + ".json");
	}

	private static Data data() {
		if (data != null) {
			return data;
		}

		Path path = file();

		if (Files.exists(path)) {
			try {
				Data loaded = GSON.fromJson(Files.readString(path), Data.class);

				if (loaded != null) {
					if (loaded.presets == null) {
						loaded.presets = new ArrayList<>();
					}

					if (loaded.recent == null) {
						loaded.recent = new LinkedHashMap<>();
					}

					data = loaded;
					return data;
				}
			} catch (Exception e) {
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to read {}", path, e);
			}
		}

		data = new Data();
		return data;
	}

	private static void save() {
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(data()));
			warned = false;
		} catch (IOException e) {
			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to save Essential presets", e);
			}
		}
	}

	public static List<Preset> presets() {
		return List.copyOf(data().presets);
	}

	@Nullable
	public static Preset preset(String name) {
		for (Preset preset : data().presets) {
			if (preset.name.equals(name)) {
				return preset;
			}
		}

		return null;
	}

	public static void put(String name, Map<CosmeticSlot, CosmeticSpec> slots) {
		Preset preset = preset(name);

		if (preset == null) {
			preset = new Preset();
			preset.name = name;
			data().presets.add(preset);
		}

		preset.slots = new LinkedHashMap<>();

		for (Map.Entry<CosmeticSlot, CosmeticSpec> entry : slots.entrySet()) {
			preset.slots.put(entry.getKey().getId(), entry.getValue().copy());
		}

		save();
	}

	public static void delete(String name) {
		if (data().presets.removeIf(preset -> preset.name.equals(name))) {
			save();
		}
	}

	public static List<String> recent(CosmeticSlot slot) {
		return List.copyOf(data().recent.getOrDefault(slot.getId(), List.of()));
	}

	public static void markUsed(CosmeticSlot slot, String cosmeticId) {
		List<String> recent = data().recent.computeIfAbsent(slot.getId(), ignored -> new ArrayList<>());
		recent.remove(cosmeticId);
		recent.addFirst(cosmeticId);

		while (recent.size() > MAX_RECENT) {
			recent.removeLast();
		}

		save();
	}
}
