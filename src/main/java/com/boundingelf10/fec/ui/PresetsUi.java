package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.config.EssentialPresets;
import com.boundingelf10.fec.cosmetics.Outfits;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import gg.essential.mod.cosmetics.CosmeticOutfit;

import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImString;

import java.util.List;
import java.util.UUID;

public final class PresetsUi {

	private static final ImString NAME = ImGuiHelper.createResizableImString("");

	private PresetsUi() { }

	public static void render(UUID player, String idSuffix, boolean open) {
		String popupId = "##fecPresetsPopup" + idSuffix;

		if (open) {
			ImGui.openPopup(popupId);
		}

		ImGui.setNextWindowSizeConstraints(300.0F, 120.0F, 520.0F, 520.0F);

		if (!ImGui.beginPopup(popupId)) {
			return;
		}

		Layout.fillWidth();

		renderSave(player, idSuffix);
		renderSaved(player, idSuffix);
		renderWardrobe(player, idSuffix);

		ImGui.endPopup();
	}

	private static void renderSave(UUID player, String idSuffix) {
		ImGuiHelper.separatorWithText("Save this look");

		String name = ImGuiHelper.getString(NAME).trim();
		boolean named = !name.isEmpty();
		float saveWidth = ImGuiHelper.calcTextWidth("Save") + ImGui.getStyle().getFramePaddingX() * 4.0F;

		ImGui.setNextItemWidth(-(saveWidth + ImGui.getStyle().getItemSpacingX()));
		ImGui.inputTextWithHint("##fecPresetName" + idSuffix, "Name", NAME);
		ImGui.sameLine();
		ImGui.beginDisabled(!named);

		if (ImGui.button("Save##fecPresetSave" + idSuffix, saveWidth, 0.0F)) {
			EssentialPresets.put(name, Outfits.effective(player));
			NAME.set("");
		}

		ImGui.endDisabled();
	}

	private static void renderSaved(UUID player, String idSuffix) {
		List<EssentialPresets.Preset> presets = EssentialPresets.presets();

		ImGuiHelper.separatorWithText("Saved");

		if (presets.isEmpty()) {
			Layout.hint("Nothing saved yet.");
			return;
		}

		float deleteWidth = ImGui.getFrameHeight();
		float applyWidth = Math.max(1.0F,
			ImGui.getContentRegionAvailX() - deleteWidth - ImGui.getStyle().getItemSpacingX());

		for (EssentialPresets.Preset preset : presets) {
			if (ImGui.button(preset.name + "###fecPresetApply" + idSuffix + preset.name, applyWidth, 0.0F)) {
				Outfits.apply(player, Outfits.fromSlotIds(preset.slots()));
				ImGui.closeCurrentPopup();
			}

			ImGui.setItemTooltip("Put this look on");
			ImGui.sameLine();

			if (ImGui.button("x###fecPresetDelete" + idSuffix + preset.name, deleteWidth, 0.0F)) {
				EssentialPresets.delete(preset.name);
				break;
			}

			ImGui.setItemTooltip("Delete this preset");
		}
	}

	private static void renderWardrobe(UUID player, String idSuffix) {
		List<CosmeticOutfit> outfits = Outfits.wardrobe();

		if (outfits.isEmpty()) {
			return;
		}

		ImGuiHelper.separatorWithText("Your wardrobe");

		for (CosmeticOutfit outfit : outfits) {
			String name = outfit.getName() == null || outfit.getName().isBlank() ? outfit.getId() : outfit.getName();

			if (Layout.wideButton(name + "###fecWardrobe" + idSuffix + outfit.getId())) {
				Outfits.apply(player, Outfits.fromWardrobe(outfit));
				ImGui.closeCurrentPopup();
			}
		}
	}
}
