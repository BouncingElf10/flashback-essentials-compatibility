package com.boundingelf10.fec.ext;


public interface EssentialCosmeticsMeta {
	int CURRENT_VERSION = 1;

	boolean fec$hasEssentialCosmetics();
	int fec$essentialCosmeticsVersion();
	void fec$setEssentialCosmetics(int version);
}
