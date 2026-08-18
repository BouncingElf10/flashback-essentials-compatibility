package com.boundingelf10.fec.cosmetics;

import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.mod.cosmetics.CapeDisabledKt;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticSetting;
import gg.essential.model.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public final class CosmeticSpec {

	@Nullable public String id;
	@Nullable public String variant;
	@Nullable public String animationVariant;
	@Nullable public String side;
	@Nullable public Float x;
	@Nullable public Float y;
	@Nullable public Float z;

	public CosmeticSpec() { }

	public static CosmeticSpec nothing() {
		return new CosmeticSpec();
	}

	public static CosmeticSpec nothingIn(CosmeticSlot slot) {
		return CosmeticSlot.CAPE.equals(slot) ? of(CapeDisabledKt.CAPE_DISABLED_COSMETIC_ID, List.of()) : nothing();
	}

	public static CosmeticSpec of(String id, List<CosmeticSetting> settings) {
		CosmeticSpec spec = new CosmeticSpec();
		spec.id = id;

		for (CosmeticSetting setting : settings) {
			switch (setting) {
				case CosmeticSetting.Variant value -> spec.variant = value.getData().getVariant();
				case CosmeticSetting.AnimationVariant value -> spec.animationVariant = value.getData().getAnimationVariant();
				case CosmeticSetting.Side value -> spec.side = value.getData().getSide().name();
				case CosmeticSetting.PlayerPositionAdjustment value -> {
					spec.x = value.getData().getX();
					spec.y = value.getData().getY();
					spec.z = value.getData().getZ();
				}
				default -> { }
			}
		}

		return spec;
	}

	public static CosmeticSpec of(@Nullable EquippedCosmeticId equipped) {
		return equipped == null ? nothing() : of(equipped.getId(), equipped.getSettings());
	}

	public CosmeticSpec copy() {
		CosmeticSpec spec = new CosmeticSpec();
		spec.id = this.id;
		spec.variant = this.variant;
		spec.animationVariant = this.animationVariant;
		spec.side = this.side;
		spec.x = this.x;
		spec.y = this.y;
		spec.z = this.z;
		return spec;
	}

	public List<CosmeticSetting> settings() {
		List<CosmeticSetting> settings = new ArrayList<>(3);

		if (this.variant != null) {
			settings.add(new CosmeticSetting.Variant(null, true, new CosmeticSetting.Variant.Data(this.variant)));
		}

		if (this.animationVariant != null) {
			settings.add(new CosmeticSetting.AnimationVariant(null, true,
				new CosmeticSetting.AnimationVariant.Data(this.animationVariant)));
		}

		Side parsed = this.sideValue();

		if (parsed != null) {
			settings.add(new CosmeticSetting.Side(null, true, new CosmeticSetting.Side.Data(parsed)));
		}

		if (this.x != null || this.y != null || this.z != null) {
			settings.add(new CosmeticSetting.PlayerPositionAdjustment(null, true,
				new CosmeticSetting.PlayerPositionAdjustment.Data(
					this.x == null ? 0.0F : this.x,
					this.y == null ? 0.0F : this.y,
					this.z == null ? 0.0F : this.z)));
		}

		return List.copyOf(settings);
	}

	@Nullable
	public Side sideValue() {
		if (this.side == null) {
			return null;
		}

		try {
			return Side.valueOf(this.side);
		} catch (IllegalArgumentException unknownSide) {
			return null;
		}
	}

	@Nullable
	public EquippedCosmeticId equipped() {
		return this.id == null ? null : new EquippedCosmeticId(this.id, this.settings());
	}

	public boolean isNothing() {
		return this.id == null;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof CosmeticSpec spec
			&& Objects.equals(this.id, spec.id)
			&& Objects.equals(this.variant, spec.variant)
			&& Objects.equals(this.animationVariant, spec.animationVariant)
			&& Objects.equals(this.side, spec.side)
			&& Objects.equals(this.x, spec.x)
			&& Objects.equals(this.y, spec.y)
			&& Objects.equals(this.z, spec.z);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.variant, this.animationVariant, this.side, this.x, this.y, this.z);
	}
}
