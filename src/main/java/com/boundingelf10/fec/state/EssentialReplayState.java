package com.boundingelf10.fec.state;

import com.boundingelf10.fec.cosmetics.CosmeticSpec;

import gg.essential.mod.cosmetics.CosmeticSlot;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public final class EssentialReplayState {
	public Map<String, Map<String, CosmeticSpec>> overrides = new LinkedHashMap<>();
	public Set<String> hidden = new LinkedHashSet<>();

	public boolean hideAllCosmetics = false;
	public boolean hideCameraPlayerCosmetics = false;
	public boolean hideEmotes = false;

	public Map<String, Map<String, CosmeticSpec>> overrides() {
		if (this.overrides == null) {
			this.overrides = new LinkedHashMap<>();
		}

		return this.overrides;
	}

	public Set<String> hidden() {
		if (this.hidden == null) {
			this.hidden = new LinkedHashSet<>();
		}

		return this.hidden;
	}

	public boolean hasOverrides(UUID player) {
		Map<String, CosmeticSpec> slots = this.overrides().get(player.toString());
		return slots != null && !slots.isEmpty();
	}

	public Map<String, CosmeticSpec> overridesOf(UUID player) {
		return this.overrides().getOrDefault(player.toString(), Map.of());
	}

	@Nullable
	public CosmeticSpec override(UUID player, CosmeticSlot slot) {
		return this.overridesOf(player).get(slot.getId());
	}

	public boolean setOverride(UUID player, CosmeticSlot slot, @Nullable CosmeticSpec spec) {
		String key = player.toString();

		if (spec == null) {
			Map<String, CosmeticSpec> slots = this.overrides().get(key);

			if (slots == null || slots.remove(slot.getId()) == null) {
				return false;
			}

			if (slots.isEmpty()) {
				this.overrides().remove(key);
			}

			return true;
		}

		Map<String, CosmeticSpec> slots = this.overrides().computeIfAbsent(key, ignored -> new LinkedHashMap<>());
		return !spec.equals(slots.put(slot.getId(), spec));
	}

	public boolean clearOverrides(UUID player) {
		return this.overrides().remove(player.toString()) != null;
	}

	public boolean isHidden(UUID player) {
		return this.hideAllCosmetics || this.hidden().contains(player.toString());
	}

	public void setHidden(UUID player, boolean hide) {
		if (hide) {
			this.hidden().add(player.toString());
		} else {
			this.hidden().remove(player.toString());
		}
	}
}
