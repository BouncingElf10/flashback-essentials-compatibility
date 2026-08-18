package com.boundingelf10.fec.cosmetics;

import com.boundingelf10.fec.FlashbackEssentialsCompatibilityClient;
import com.boundingelf10.fec.state.EssentialState;

import gg.essential.Essential;
import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.mod.cosmetics.CosmeticOutfit;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;

public final class Outfits {

	private Outfits() { }

	public static Map<CosmeticSlot, CosmeticSpec> effective(UUID player) {
		Map<CosmeticSlot, CosmeticSpec> look = new LinkedHashMap<>();
		Map<CosmeticSlot, CosmeticSpec> overrides = CosmeticsOverrides.of(player);

		IngameEquippedOutfitsManager manager =
			EssentialCosmetics.ingameManagerFor(Minecraft.getInstance().getConnection());

		for (CosmeticSlot slot : CosmeticSlots.wearable()) {
			CosmeticSpec override = overrides.get(slot);

			if (override != null) {
				if (!override.isNothing()) {
					look.put(slot, override);
				}

				continue;
			}

			EquippedCosmeticId recorded = RecordedCosmetics.get(player, slot);

			if (recorded == null && manager != null && CosmeticsPipeline.applied(player, slot) == null) {
				recorded = EssentialCosmetics.outfitOf(manager, player).getCosmetics().get(slot);
			}

			if (recorded != null) {
				look.put(slot, CosmeticSpec.of(recorded));
			}
		}

		return look;
	}

	public static void apply(UUID player, Map<CosmeticSlot, CosmeticSpec> look) {
		EssentialState.edit(state -> {
			for (CosmeticSlot slot : CosmeticSlots.wearable()) {
				CosmeticSpec spec = look.get(slot);
				state.setOverride(player, slot, spec == null ? CosmeticSpec.nothingIn(slot) : spec.copy());
			}
		});

		CosmeticsPipeline.refresh();
	}

	public static Map<CosmeticSlot, CosmeticSpec> fromSlotIds(Map<String, CosmeticSpec> stored) {
		Map<CosmeticSlot, CosmeticSpec> look = new LinkedHashMap<>();

		for (Map.Entry<String, CosmeticSpec> entry : stored.entrySet()) {
			look.put(CosmeticSlots.of(entry.getKey()), entry.getValue());
		}

		return look;
	}

	public static List<CosmeticOutfit> wardrobe() {
		try {
			return List.copyOf(Essential.getInstance()
				.getConnectionManager()
				.getOutfitManager()
				.getOutfits()
				.getUntracked());
		} catch (Throwable e) {
			FlashbackEssentialsCompatibilityClient.LOGGER.debug("Essential's wardrobe outfits aren't available", e);
			return List.of();
		}
	}

	public static Map<CosmeticSlot, CosmeticSpec> fromWardrobe(CosmeticOutfit outfit) {
		Map<CosmeticSlot, CosmeticSpec> look = new LinkedHashMap<>();
		Map<String, List<CosmeticSetting>> settings = outfit.getCosmeticSettings();

		for (Map.Entry<CosmeticSlot, String> entry : outfit.getEquippedCosmetics().entrySet()) {
			if (CosmeticSlot.EMOTE.equals(entry.getKey())) {
				continue;
			}

			List<CosmeticSetting> forCosmetic = settings == null ? null : settings.get(entry.getValue());
			look.put(entry.getKey(), CosmeticSpec.of(entry.getValue(),
				forCosmetic == null ? List.of() : new ArrayList<>(forCosmetic)));
		}

		return look;
	}
}
