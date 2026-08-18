package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import gg.essential.Essential;
import gg.essential.mod.cosmetics.CapeDisabledKt;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.cosmetics.Cosmetic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class CosmeticCatalog {
	public enum Status {
		READY("Connected to Essential."),
		LOADING("Essential is still loading its cosmetics."),
		NOT_CONNECTED("Not signed in to Essential, so its cosmetics aren't available."),
		UNAVAILABLE("Essential isn't responding; see the log for details.");

		private final String message;

		Status(String message) {
			this.message = message;
		}

		public String message() {
			return this.message;
		}

		public boolean isUsable() {
			return this == READY;
		}
	}

	private static final long REFRESH_INTERVAL_MILLIS = 5000L;

	private static volatile Map<CosmeticSlot, List<Cosmetic>> bySlot = Map.of();
	private static volatile Set<String> unlocked = Set.of();
	private static volatile Status status = Status.LOADING;
	private static volatile long lastRefresh = 0L;
	private static boolean warned = false;

	private CosmeticCatalog() {
	}

	public static List<Cosmetic> forSlot(CosmeticSlot slot) {
		refreshIfStale();
		return bySlot.getOrDefault(slot, List.of());
	}

	public static Status status() {
		refreshIfStale();
		return status;
	}

	public static Set<String> unlocked() {
		refreshIfStale();
		return unlocked;
	}

	@Nullable
	public static Cosmetic find(CosmeticSlot slot, @Nullable String id) {
		if (id == null) {
			return null;
		}

		for (Cosmetic candidate : forSlot(slot)) {
			if (candidate.getId().equals(id)) {
				return candidate;
			}
		}

		return null;
	}

	private static void refreshIfStale() {
		long now = System.currentTimeMillis();

		if (now - lastRefresh <= REFRESH_INTERVAL_MILLIS) {
			return;
		}

		lastRefresh = now;

		try {
			ConnectionManager connection = Essential.getInstance().getConnectionManager();
			CosmeticsManager cosmeticsManager = connection.getCosmeticsManager();

			List<Cosmetic> cosmetics = cosmeticsManager
				.getCosmeticsData()
				.getCosmetics()
				.getUntracked();

			Map<CosmeticSlot, List<Cosmetic>> grouped = new LinkedHashMap<>();

			for (Cosmetic cosmetic : cosmetics) {
				if (CapeDisabledKt.CAPE_DISABLED_COSMETIC_ID.equals(cosmetic.getId())) {
					continue;
				}

				grouped.computeIfAbsent(cosmetic.getSlot(), ignored -> new ArrayList<>()).add(cosmetic);
			}

			for (List<Cosmetic> group : grouped.values()) {
				group.sort(Comparator.comparing(CosmeticCatalog::displayNameOf, String.CASE_INSENSITIVE_ORDER));
			}

			bySlot = grouped;
			unlocked = Set.copyOf(cosmeticsManager.getUnlockedCosmetics().getUntracked());
			warned = false;

			if (!cosmetics.isEmpty()) {
				status = Status.READY;
			} else if (!connection.isAuthenticated()) {
				status = Status.NOT_CONNECTED;
			} else {
				status = Status.LOADING;
			}
		} catch (Throwable e) {
			status = Status.UNAVAILABLE;

			if (!warned) {
				warned = true;
				FlashbackEssentialsCompatibilityClient.LOGGER.error("Failed to read Essential's cosmetic catalog", e);
			}
		}
	}

	public static String displayNameOf(Cosmetic cosmetic) {
		String name = cosmetic.getDisplayName();
		return name == null || name.isBlank() ? cosmetic.getId() : name;
	}

	public static String categoryOf(Cosmetic cosmetic) {
		Map<String, Integer> categories = cosmetic.getCategories();

		if (categories == null || categories.isEmpty()) {
			return "Other";
		}

		String best = null;
		int bestWeight = Integer.MIN_VALUE;

		for (Map.Entry<String, Integer> entry : categories.entrySet()) {
			int weight = entry.getValue() == null ? 0 : entry.getValue();

			if (best == null || weight > bestWeight) {
				best = entry.getKey();
				bestWeight = weight;
			}
		}

		return prettify(best);
	}

	private static String prettify(String category) {
		String[] words = category.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
		StringBuilder pretty = new StringBuilder(category.length());

		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}

			if (!pretty.isEmpty()) {
				pretty.append(' ');
			}

			pretty.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return pretty.isEmpty() ? "Other" : pretty.toString();
	}
}
