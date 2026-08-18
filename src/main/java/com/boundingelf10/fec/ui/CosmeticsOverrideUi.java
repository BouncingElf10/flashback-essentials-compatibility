package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.cosmetics.CosmeticCatalog;
import com.boundingelf10.fec.cosmetics.CosmeticSlots;
import com.boundingelf10.fec.cosmetics.CosmeticSpec;
import com.boundingelf10.fec.cosmetics.CosmeticsOverrides;
import com.boundingelf10.fec.cosmetics.CosmeticsPipeline;
import com.boundingelf10.fec.cosmetics.EssentialCosmetics;
import com.boundingelf10.fec.cosmetics.RecordedCosmetics;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.boundingelf10.fec.state.EssentialState;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.mod.cosmetics.CapeDisabledKt;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;

import imgui.moulberry90.ImGui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

public final class CosmeticsOverrideUi {

	private CosmeticsOverrideUi() {
	}

	public static void render(Entity entity) {
		if (!(entity instanceof AbstractClientPlayer player)) {
			return;
		}

		if (!ImGui.collapsingHeader("Essential Cosmetics")) {
			return;
		}

		Layout.pinWidth();

		renderFor(player.getUUID(), "Popup");
	}

	public static void renderFor(UUID uuid, String idSuffix) {
		CosmeticCatalog.Status status = CosmeticCatalog.status();

		if (!status.isUsable()) {
			Layout.warning(status.message());
		}

		EssentialReplayState state = EssentialState.current();

		if (state == null) {
			Layout.hint("Open a replay to change cosmetics.");
			return;
		}

		if (ImGui.checkbox("Hide this player's cosmetics##fecHide" + idSuffix, state.isHidden(uuid))) {
			boolean hidden = state.isHidden(uuid);
			EssentialState.edit(current -> current.setHidden(uuid, !hidden));
			CosmeticsPipeline.refresh();
		}

		boolean overridden = CosmeticsOverrides.hasAny(uuid);
		int pressed = Layout.buttonRow(new boolean[]{true, overridden},
			"Presets##fecPresets" + idSuffix,
			"Reset cosmetics##fecReset" + idSuffix);

		if (pressed == 1) {
			CosmeticsOverrides.clear(uuid);
		}

		PresetsUi.render(uuid, idSuffix, pressed == 0);

		List<CosmeticSlot> slots = new ArrayList<>();

		for (CosmeticSlot slot : CosmeticSlots.wearable()) {
			if (!CosmeticCatalog.forSlot(slot).isEmpty()) {
				slots.add(slot);
			}
		}

		if (!slots.isEmpty()) {
			ImGui.separator();
		}

		float labelColumn = labelColumn(slots);

		for (CosmeticSlot slot : slots) {
			renderSlot(uuid, slot, idSuffix, labelColumn);
		}

		ImGui.separator();
		Layout.hint("Emotes are set with Essential Emote tracks on the timeline.");
	}

	private static float labelColumn(List<CosmeticSlot> slots) {
		List<String> labels = new ArrayList<>();

		for (CosmeticSlot slot : slots) {
			labels.add(CosmeticSlots.displayNameOf(slot));
		}

		return Math.max(Layout.labelColumn(labels), CosmeticSettingsUi.labelColumn());
	}

	private static void renderSlot(UUID uuid, CosmeticSlot slot, String idSuffix, float labelColumn) {
		CosmeticSpec override = CosmeticsOverrides.get(uuid, slot);
		String id = slot.getId() + idSuffix;

		CosmeticPicker.render(
			id,
			CosmeticSlots.displayNameOf(slot),
			labelColumn,
			slot,
			override == null,
			override == null ? null : override.id,
			"From replay" + describeRecorded(uuid, slot),
			CosmeticSlot.CAPE.equals(slot) ? "No cape" : "None",
			new CosmeticPicker.Sink() {
				@Override
				public void fromReplay() {
					CosmeticsOverrides.set(uuid, slot, null);
				}

				@Override
				public void nothing() {
					CosmeticsOverrides.set(uuid, slot, CosmeticSpec.nothingIn(slot));
				}

				@Override
				public void cosmetic(Cosmetic cosmetic) {
					CosmeticsOverrides.set(uuid, slot, CosmeticSpec.of(cosmetic.getId(), List.of()));
				}
			});

		CosmeticSettingsUi.render(id, slot, effective(uuid, slot), labelColumn,
			spec -> CosmeticsOverrides.set(uuid, slot, spec));
	}

	private static CosmeticSpec effective(UUID uuid, CosmeticSlot slot) {
		CosmeticSpec override = CosmeticsOverrides.get(uuid, slot);
		return override != null ? override : CosmeticSpec.of(recordedEquipped(uuid, slot));
	}

	private static String describeRecorded(UUID uuid, CosmeticSlot slot) {
		EquippedCosmeticId recorded = recordedEquipped(uuid, slot);
		boolean isCape = CosmeticSlot.CAPE.equals(slot);

		if (recorded == null) {
			return isCape ? " (own cape)" : " (nothing)";
		}

		if (CapeDisabledKt.CAPE_DISABLED_COSMETIC_ID.equals(recorded.getId())) {
			return " (none)";
		}

		Cosmetic cosmetic = CosmeticCatalog.find(slot, recorded.getId());

		if (cosmetic != null) {
			return " (" + CosmeticCatalog.displayNameOf(cosmetic) + ")";
		}

		return CosmeticCatalog.status().isUsable()
			? " (" + recorded.getId() + ")"
			: " (unknown; " + CosmeticCatalog.status().message() + ")";
	}

	@Nullable
	private static EquippedCosmeticId recordedEquipped(UUID uuid, CosmeticSlot slot) {
		EquippedCosmeticId recorded = RecordedCosmetics.get(uuid, slot);

		if (recorded != null) {
			return recorded;
		}

		if (CosmeticsPipeline.applied(uuid, slot) != null) {
			return null;
		}

		IngameEquippedOutfitsManager manager =
			EssentialCosmetics.ingameManagerFor(Minecraft.getInstance().getConnection());

		if (manager == null) {
			return null;
		}

		return EssentialCosmetics.outfitOf(manager, uuid).getCosmetics().get(slot);
	}
}
