package com.boundingelf10.fec.cosmetics;

import gg.essential.mod.cosmetics.CosmeticSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CosmeticSlots {

	private CosmeticSlots() { }

	public static CosmeticSlot of(String id) {
		return CosmeticSlot.Companion.of(id);
	}

	public static List<CosmeticSlot> wearable() {
		List<CosmeticSlot> slots = new ArrayList<>();

		for (CosmeticSlot slot : CosmeticSlot.Companion.values()) {
			if (!CosmeticSlot.EMOTE.equals(slot)) {
				slots.add(slot);
			}
		}

		return slots;
	}

	public static String displayNameOf(CosmeticSlot slot) {
		String id = slot.getId().replace('_', ' ').toLowerCase(Locale.ROOT);
		return id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1);
	}
}
