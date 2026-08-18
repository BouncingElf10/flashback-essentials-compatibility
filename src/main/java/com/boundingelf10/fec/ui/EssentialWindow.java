package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.cosmetics.CosmeticCatalog;
import com.boundingelf10.fec.cosmetics.CosmeticsOverrides;
import com.boundingelf10.fec.cosmetics.CosmeticsPipeline;
import com.boundingelf10.fec.cosmetics.Outfits;
import com.boundingelf10.fec.state.EssentialReplayState;
import com.boundingelf10.fec.state.EssentialState;
import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import imgui.moulberry90.ImGui;
import imgui.moulberry90.ImGuiViewport;
import imgui.moulberry90.type.ImBoolean;

import java.util.List;
import java.util.UUID;

/**
 * A dockable window listing every player in the replay, so cosmetics can be reached without
 * having to click an entity that may be off-screen — and so a look can be copied from one
 * player to another in one go.
 */
public final class EssentialWindow {

	private static final ImBoolean OPEN = new ImBoolean(false);
	private static final int[] COPY_FROM = new int[1];
	private static final int[] COPY_TO = new int[1];

	private static boolean newlyOpened = false;

	private EssentialWindow() {
	}

	public static void toggle() {
		OPEN.set(!OPEN.get());
		newlyOpened = OPEN.get();
	}

	public static void render() {
		if (!OPEN.get() || !Flashback.isInReplay()) {
			return;
		}

		if (newlyOpened) {
			newlyOpened = false;
			ImGuiViewport viewport = ImGui.getMainViewport();
			ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), 8, 0.5F, 0.5F);
			ImGui.setNextWindowSize(360.0F, 480.0F, 8);
		}

		ImGui.setNextWindowSizeConstraints(280.0F, 120.0F, 5000.0F, 5000.0F);

		if (ImGui.begin("Essential###FecEssential", OPEN)) {
			renderInner();
		}

		ImGui.end();
	}

	private static void renderInner() {
		Layout.fillWidth();

		CosmeticCatalog.Status status = CosmeticCatalog.status();

		if (status.isUsable()) {
			Layout.hint(status.message());
		} else {
			Layout.warning(status.message());
		}

		EssentialReplayState state = EssentialState.current();

		if (state == null) {
			Layout.hint("No replay is open.");
			return;
		}

		ImGuiHelper.separatorWithText("Everyone");
		renderToggles(state);
		renderCopy();
		ImGuiHelper.separatorWithText("Players");
		renderPlayers(state);
	}

	private static void renderToggles(EssentialReplayState state) {
		if (ImGui.checkbox("Hide all cosmetics##fecHideAll", state.hideAllCosmetics)) {
			EssentialState.edit(current -> current.hideAllCosmetics = !current.hideAllCosmetics);
			CosmeticsPipeline.refresh();
		}

		if (ImGui.checkbox("Hide the camera player's cosmetics##fecHideCamera", state.hideCameraPlayerCosmetics)) {
			EssentialState.edit(current -> current.hideCameraPlayerCosmetics = !current.hideCameraPlayerCosmetics);
			CosmeticsPipeline.refresh();
		}

		if (ImGui.checkbox("Hide emotes##fecHideEmotes", state.hideEmotes)) {
			EssentialState.edit(current -> current.hideEmotes = !current.hideEmotes);
		}

		Layout.help("Suppresses emotes recorded in the replay and the ones on Essential Emote tracks, along with the particles and sounds they play.");

		int pressed = Layout.buttonRow("Clear all overrides##fecClearAll", "Show everyone##fecShowAll");

		if (pressed == 0) {
			EssentialState.edit(current -> current.overrides().clear());
			CosmeticsPipeline.refresh();
		} else if (pressed == 1) {
			EssentialState.edit(current -> {
				current.hidden().clear();
				current.hideAllCosmetics = false;
			});
			CosmeticsPipeline.refresh();
		}
	}

	private static void renderCopy() {
		List<UUID> players = PlayerChoice.present();

		if (players.size() < 2) {
			return;
		}

		ImGuiHelper.separatorWithText("Copy a look");

		String[] labels = new String[players.size()];

		for (int i = 0; i < players.size(); i++) {
			labels[i] = PlayerChoice.describe(players.get(i));
		}

		COPY_FROM[0] = Math.min(COPY_FROM[0], players.size() - 1);
		COPY_TO[0] = Math.min(COPY_TO[0], players.size() - 1);

		float column = Layout.labelColumn("From", "To");

		Layout.label("From", column);
		ImGuiHelper.combo("##fecCopyFrom", COPY_FROM, labels);
		Layout.label("To", column);
		ImGuiHelper.combo("##fecCopyTo", COPY_TO, labels);

		boolean sensible = COPY_FROM[0] != COPY_TO[0];

		ImGui.beginDisabled(!sensible);

		if (Layout.wideButton("Copy look##fecCopyLook")) {
			Outfits.apply(players.get(COPY_TO[0]), Outfits.effective(players.get(COPY_FROM[0])));
		}

		ImGui.endDisabled();
	}

	private static void renderPlayers(EssentialReplayState state) {
		List<UUID> players = PlayerChoice.present();

		if (players.isEmpty()) {
			Layout.hint("No players in the replay.");
			return;
		}

		for (UUID player : players) {
			ImGui.pushID(player.toString());
			int overrides = CosmeticsOverrides.of(player).size();
			String tag = overrides > 0 ? overrides + " overridden" : "";

			if (state.isHidden(player)) {
				tag = tag.isEmpty() ? "hidden" : "hidden, " + tag;
			}

			boolean open = ImGui.treeNode(PlayerChoice.describe(player) + "###fecPlayer");

			if (!tag.isEmpty()) {
				Layout.rightAlignedHint(tag);
			}

			if (open) {
				CosmeticsOverrideUi.renderFor(player, "Window" + player);
				ImGui.treePop();
			}

			ImGui.popID();
		}
	}
}
