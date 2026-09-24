package com.boundingelf10.fec.ui;

import com.boundingelf10.fec.emotes.EmoteKeyframe;
import com.moulberry.flashback.editor.ui.ImGuiHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import com.mojang.authlib.GameProfile;

public final class PlayerChoice {

	private static final int[] SCRATCH = new int[1];

	// GameProfile became a record in 1.21.9 (getName() -> name())
	private static final MethodHandle PROFILE_NAME = profileAccessor("name", "getName");

	private PlayerChoice() { }

	public static void render(String id, UUID current, float labelColumn, Consumer<UUID> sink) {
		List<UUID> players = players(current);
		String[] labels = new String[players.size()];

		for (int i = 0; i < players.size(); i++) {
			labels[i] = describe(players.get(i));
		}

		int selected = Math.max(0, players.indexOf(current));
		SCRATCH[0] = selected;
		Layout.label("Player", labelColumn);

		if (ImGuiHelper.combo("##fecPlayer" + id, SCRATCH, labels) && SCRATCH[0] != selected) {
			sink.accept(players.get(SCRATCH[0]));
		}
	}

	public static UUID defaultPlayer() {
		List<UUID> players = players(EmoteKeyframe.NO_PLAYER);
		return players.size() > 1 ? players.get(1) : EmoteKeyframe.NO_PLAYER;
	}

	public static List<UUID> present() {
		Set<UUID> present = new LinkedHashSet<>();

		Minecraft minecraft = Minecraft.getInstance();
		ClientPacketListener connection = minecraft.getConnection();

		if (connection != null) {
			present.addAll(connection.getOnlinePlayerIds());
		}

		ClientLevel level = minecraft.level;

		if (level != null) {
			for (Player player : level.players()) {
				present.add(player.getUUID());
			}
		}

		List<UUID> sorted = new ArrayList<>(present);
		sorted.sort(Comparator.comparing(PlayerChoice::describe, String.CASE_INSENSITIVE_ORDER));
		return sorted;
	}

	private static List<UUID> players(UUID required) {
		Set<UUID> uuids = new LinkedHashSet<>();
		uuids.add(EmoteKeyframe.NO_PLAYER);
		uuids.addAll(present());
		uuids.add(required);
		return List.copyOf(uuids);
	}

	public static String describe(UUID uuid) {
		if (EmoteKeyframe.NO_PLAYER.equals(uuid)) {
			return "(nobody)";
		}

		ClientPacketListener connection = Minecraft.getInstance().getConnection();

		if (connection != null) {
			PlayerInfo info = connection.getPlayerInfo(uuid);

			String name = info == null ? null : profileName(info);

			if (name != null) {
				return name;
			}
		}

		return uuid.toString().substring(0, 8);
	}

	private static String profileName(PlayerInfo info) {
		try {
			return PROFILE_NAME == null ? null : (String) PROFILE_NAME.invokeExact(info.getProfile());
		} catch (Throwable t) {
			return null;
		}
	}

	private static MethodHandle profileAccessor(String... names) {
		for (String name : names) {
			try {
				return MethodHandles.publicLookup().findVirtual(GameProfile.class, name, MethodType.methodType(String.class));
			} catch (ReflectiveOperationException ignored) {
			}
		}

		return null;
	}
}
