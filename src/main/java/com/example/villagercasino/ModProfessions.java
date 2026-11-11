package com.example.villagercasino;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class ModProfessions {
	public static final ResourceKey<PoiType> CASINO_POI =
			ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, VillagerCasinoMod.id("casino"));
	public static final ResourceKey<VillagerProfession> CROUPIER =
			ResourceKey.create(Registries.VILLAGER_PROFESSION, VillagerCasinoMod.id("croupier"));

	private ModProfessions() {
	}

	public static void register() {
		// WICHTIG: ALLE Zustaende des Blocks gehoeren zum POI-Typ (Berufsblock UND Spielautomat).
		// Nur so behaelt der Dorfbewohner seinen Job, nachdem sich der Block verwandelt hat.
		Set<BlockState> states = ImmutableSet.copyOf(
				ModBlocks.CASINO_BLOCK.getStateDefinition().getPossibleStates());

		// 1 Ticket (= 1 Dorfbewohner pro Block), Suchdistanz 1 - wie bei allen Vanilla-Berufen.
		// Die Methode ist in Vanilla private; Fabric API (>= 0.163 fuer 26.3) macht sie per
		// Transitive Access Widener oeffentlich.
		PoiTypes.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, CASINO_POI, states, 1, 1);

		// Seit 26.1 sind Trades datengetrieben: Berufs-Level -> Trade-Set (data/.../trade_set/...)
		Int2ObjectMap<ResourceKey<TradeSet>> tradeSets = new Int2ObjectOpenHashMap<>();
		for (int level = 1; level <= 5; level++) {
			tradeSets.put(level, ResourceKey.create(
					Registries.TRADE_SET,
					VillagerCasinoMod.id("croupier/level_" + level)));
		}

		Registry.register(
				BuiltInRegistries.VILLAGER_PROFESSION,
				CROUPIER,
				new VillagerProfession(
						Component.translatable("entity.villagercasino.villager.croupier"),
						holder -> holder.is(CASINO_POI), // Block, an dem der Beruf "haengt"
						holder -> holder.is(CASINO_POI), // Block, den Arbeitslose annehmen koennen
						ImmutableSet.of(),
						ImmutableSet.of(),
						SoundEvents.VILLAGER_WORK_CLERIC,
						tradeSets));
	}
}
