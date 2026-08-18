package com.boundingelf10.fec.cosmetics;

import gg.essential.mod.cosmetics.settings.CosmeticProperty;
import gg.essential.network.cosmetics.Cosmetic;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public final class CosmeticSettings {

	private CosmeticSettings() { }

	public static List<CosmeticProperty.Variants.Variant> variantsOf(Cosmetic cosmetic) {
		List<CosmeticProperty.Variants.Variant> variants = cosmetic.getVariants();
		return variants == null ? List.of() : variants;
	}

	public static boolean supportsSides(Cosmetic cosmetic) {
		return cosmetic.getDefaultSide() != null;
	}

	@Nullable
	public static CosmeticProperty.PositionRange.Data positionRangeOf(Cosmetic cosmetic) {
		for (CosmeticProperty property : cosmetic.getAllProperties()) {
			if (property instanceof CosmeticProperty.PositionRange range) {
				return range.getData();
			}
		}

		return null;
	}

	public static float rangeMin(@Nullable Float bound) {
		return bound == null ? -16.0F : bound;
	}

	public static float rangeMax(@Nullable Float bound) {
		return bound == null ? 16.0F : bound;
	}
}
