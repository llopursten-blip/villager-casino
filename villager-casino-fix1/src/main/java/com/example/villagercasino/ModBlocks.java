package com.example.villagercasino;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final ResourceKey<Block> CASINO_BLOCK_KEY =
			ResourceKey.create(Registries.BLOCK, VillagerCasinoMod.id("casino_block"));
	public static final ResourceKey<Item> CASINO_BLOCK_ITEM_KEY =
			ResourceKey.create(Registries.ITEM, VillagerCasinoMod.id("casino_block"));

	public static Block CASINO_BLOCK;
	public static Item CASINO_BLOCK_ITEM;

	private ModBlocks() {
	}

	public static void register() {
		CASINO_BLOCK = Registry.register(
				BuiltInRegistries.BLOCK,
				CASINO_BLOCK_KEY,
				new CasinoBlock(BlockBehaviour.Properties.of()
						.setId(CASINO_BLOCK_KEY)
						.mapColor(MapColor.COLOR_RED)
						.strength(3.0F, 6.0F)
						.sound(SoundType.METAL)
						// Als Spielautomat leuchtet der Block leicht
						.lightLevel(state -> state.getValue(CasinoBlock.SLOT_MACHINE) ? 7 : 0)));

		CASINO_BLOCK_ITEM = Registry.register(
				BuiltInRegistries.ITEM,
				CASINO_BLOCK_ITEM_KEY,
				new BlockItem(CASINO_BLOCK, new Item.Properties()
						.setId(CASINO_BLOCK_ITEM_KEY)
						.useBlockDescriptionPrefix()));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> output.accept(CASINO_BLOCK_ITEM));
	}
}
