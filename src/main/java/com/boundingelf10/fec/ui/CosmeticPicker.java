package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.config.EssentialPresets;
import com.boundingelf10.fec.cosmetics.CosmeticCatalog;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.network.cosmetics.Cosmetic;

import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class CosmeticPicker {

	public interface Sink {
		void fromReplay();
		void nothing();
		void cosmetic(Cosmetic cosmetic);
	}

	private static final ImString FILTER = ImGuiHelper.createResizableImString("");
	private static final float LIST_HEIGHT = 320.0F;

	private static String openPicker = null;
	private static boolean justOpened = false;
	private static boolean ownedOnly = false;

	private CosmeticPicker() { }

	public static void render(String id, String label, float labelColumn, CosmeticSlot slot, boolean fromReplay, @Nullable String selectedId, @Nullable String fromReplayLabel, String nothingLabel, Sink sink) {
		String popupId = "##fecPickerPopup" + id;
		Cosmetic selected = CosmeticCatalog.find(slot, selectedId);
		String current;

		if (fromReplay && fromReplayLabel != null) {
			current = fromReplayLabel;
		} else if (selected != null) {
			current = CosmeticCatalog.displayNameOf(selected);
		} else if (selectedId != null) {
			current = selectedId;
		} else {
			current = nothingLabel;
		}

		float width = Layout.label(label, labelColumn);

		if (ImGui.button(current + "###fecPicker" + id, width, 0.0F)) {
			openPicker = id;
			justOpened = true;
			FILTER.set("");
			ImGui.openPopup(popupId);
		}

		ImGui.setItemTooltip(current);

		ImGui.setNextWindowSizeConstraints(320.0F, 160.0F, 640.0F, 640.0F);

		if (!ImGui.beginPopup(popupId)) {
			return;
		}

		Layout.fillWidth();

		boolean focus = justOpened && id.equals(openPicker);
		justOpened = false;

		if (focus) {
			ImGui.setKeyboardFocusHere();
		}

		ImGui.setNextItemWidth(-1.0F);
		ImGui.inputTextWithHint("##fecPickerFilter", "Search", FILTER);

		Set<String> unlocked = CosmeticCatalog.unlocked();

		if (!unlocked.isEmpty() && ImGui.checkbox("Owned only##fecPickerOwned", ownedOnly)) {
			ownedOnly = !ownedOnly;
		}

		CosmeticCatalog.Status status = CosmeticCatalog.status();

		if (!status.isUsable()) {
			Layout.hint(status.message());
		}

		ImGui.separator();

		if (ImGui.beginChild("##fecPickerList" + id, 0.0F, LIST_HEIGHT)) {
			renderEntries(slot, fromReplay, selectedId, fromReplayLabel, nothingLabel, unlocked, sink);
		}

		ImGui.endChild();
		ImGui.endPopup();
	}

	private static void renderEntries(CosmeticSlot slot, boolean fromReplay, @Nullable String selectedId, @Nullable String fromReplayLabel, String nothingLabel, Set<String> unlocked, Sink sink) {
		String filter = ImGuiHelper.getString(FILTER).trim().toLowerCase(Locale.ROOT);
		boolean unfiltered = filter.isEmpty();

		if (unfiltered && fromReplayLabel != null && ImGui.selectable(fromReplayLabel + "##fecPickerReplay", fromReplay)) {
			sink.fromReplay();
			ImGui.closeCurrentPopup();
		}

		if (unfiltered && ImGui.selectable(nothingLabel + "##fecPickerNothing", !fromReplay && selectedId == null)) {
			sink.nothing();
			ImGui.closeCurrentPopup();
		}

		List<Cosmetic> options = new ArrayList<>();

		for (Cosmetic cosmetic : CosmeticCatalog.forSlot(slot)) {
			if (ownedOnly && !unlocked.isEmpty() && !unlocked.contains(cosmetic.getId())) {
				continue;
			}

			if (unfiltered || matches(cosmetic, filter)) {
				options.add(cosmetic);
			}
		}

		if (options.isEmpty()) {
			Layout.hint(unfiltered
				? CosmeticCatalog.status().isUsable() ? "Nothing in this slot." : CosmeticCatalog.status().message()
				: "Nothing matches.");
			return;
		}

		if (unfiltered) {
			renderRecent(slot, selectedId, options, sink);
			renderByCategory(options, selectedId, slot, sink);
			return;
		}

		for (Cosmetic cosmetic : options) {
			renderEntry(cosmetic, selectedId, slot, unlocked, sink);
		}
	}

	private static void renderRecent(CosmeticSlot slot, @Nullable String selectedId, List<Cosmetic> options, Sink sink) {
		List<String> recent = EssentialPresets.recent(slot);

		if (recent.isEmpty()) {
			return;
		}

		List<Cosmetic> found = new ArrayList<>();

		for (String id : recent) {
			for (Cosmetic cosmetic : options) {
				if (cosmetic.getId().equals(id)) {
					found.add(cosmetic);
					break;
				}
			}
		}

		if (found.isEmpty()) {
			return;
		}

		ImGuiHelper.separatorWithText("Recent");

		for (Cosmetic cosmetic : found) {
			renderEntry(cosmetic, selectedId, slot, Set.of(), sink);
		}
	}

	private static void renderByCategory(List<Cosmetic> options, @Nullable String selectedId, CosmeticSlot slot, Sink sink) {
		Map<String, List<Cosmetic>> byCategory = new LinkedHashMap<>();

		for (Cosmetic cosmetic : options) {
			byCategory.computeIfAbsent(CosmeticCatalog.categoryOf(cosmetic), ignored -> new ArrayList<>()).add(cosmetic);
		}

		List<String> categories = new ArrayList<>(byCategory.keySet());
		categories.sort(String.CASE_INSENSITIVE_ORDER);

		for (String category : categories) {
			ImGuiHelper.separatorWithText(category);

			for (Cosmetic cosmetic : byCategory.get(category)) {
				renderEntry(cosmetic, selectedId, slot, Set.of(), sink);
			}
		}
	}

	private static void renderEntry(Cosmetic cosmetic, @Nullable String selectedId, CosmeticSlot slot, Set<String> unlocked, Sink sink) {
		String name = CosmeticCatalog.displayNameOf(cosmetic);

		if (ImGui.selectable(name + "##fecPickerItem" + cosmetic.getId(), cosmetic.getId().equals(selectedId))) {
			EssentialPresets.markUsed(slot, cosmetic.getId());
			sink.cosmetic(cosmetic);
			ImGui.closeCurrentPopup();
		}

		if (ImGui.isItemHovered()) {
			ImGui.setItemTooltip(name + "\n" + cosmetic.getId());
		}
	}

	private static boolean matches(Cosmetic cosmetic, String filter) {
		return CosmeticCatalog.displayNameOf(cosmetic).toLowerCase(Locale.ROOT).contains(filter)
			|| cosmetic.getId().toLowerCase(Locale.ROOT).contains(filter)
			|| CosmeticCatalog.categoryOf(cosmetic).toLowerCase(Locale.ROOT).contains(filter);
	}
}
