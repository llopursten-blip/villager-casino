package com.example.villagercasino;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/**
 * Berufsblock fuer den Croupier-Dorfbewohner.
 *
 * <ul>
 *   <li>{@code slot_machine=false}: normaler Berufsblock ("Casino-Tisch")</li>
 *   <li>{@code slot_machine=true}: Sobald ein Dorfbewohner den Block als Arbeitsplatz annimmt,
 *       verwandelt er sich dauerhaft in einen Spielautomaten.</li>
 * </ul>
 *
 * Es ist bewusst EIN Block mit zwei Zustaenden: Beide Zustaende gehoeren zum selben POI-Typ,
 * dadurch bleibt der Arbeitsplatz des Dorfbewohners nach der Verwandlung gueltig.
 */
public class CasinoBlock extends Block {
	public static final BooleanProperty SLOT_MACHINE = BooleanProperty.create("slot_machine");
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Wie oft (in Ticks) ein noch freier Block nach einem Dorfbewohner Ausschau haelt. */
	private static final int CHECK_INTERVAL_TICKS = 20;
	/** Ein Dorfbewohner muss sich beim Annehmen des Jobs ohnehin direkt am Block befinden. */
	private static final double CLAIM_RADIUS = 4.0;

	public CasinoBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(SLOT_MACHINE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SLOT_MACHINE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	// ------------------------------------------------------------------
	// Verwandlung: Berufsblock -> Spielautomat
	// ------------------------------------------------------------------

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!state.getValue(SLOT_MACHINE) && !oldState.is(this)) {
			level.scheduleTick(pos, this, CHECK_INTERVAL_TICKS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(SLOT_MACHINE)) {
			return;
		}

		if (isClaimedByVillager(level, pos)) {
			level.setBlock(pos, state.setValue(SLOT_MACHINE, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					12, 0.3, 0.3, 0.3, 0.0);
			return;
		}

		// Noch niemand hier -> spaeter nochmal nachsehen
		level.scheduleTick(pos, this, CHECK_INTERVAL_TICKS);
	}

	/** True, sobald ein Dorfbewohner diesen Block als Arbeitsplatz (JOB_SITE) gespeichert hat. */
	private static boolean isClaimedByVillager(ServerLevel level, BlockPos pos) {
		GlobalPos jobSite = GlobalPos.of(level.dimension(), pos);
		AABB area = new AABB(pos).inflate(CLAIM_RADIUS);

		for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
			Optional<GlobalPos> memory = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
			if (memory.isPresent() && memory.get().equals(jobSite)) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------
	// Spielautomat: Smaragd rein, Walzen drehen, Gewinn raus
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		// Solange kein Dorfbewohner den Block angenommen hat, ist es nur ein Berufsblock.
		if (!state.getValue(SLOT_MACHINE)) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS; // Client: nur Animation, Logik laeuft auf dem Server
		}

		ItemStack held = player.getMainHandItem();
		if (!held.is(Items.EMERALD)) {
			player.displayClientMessage(Component.translatable("message.villagercasino.insert_emerald"), true);
			return InteractionResult.SUCCESS;
		}

		if (!player.hasInfiniteMaterials()) {
			held.shrink(1);
		}

		RandomSource random = serverLevel.getRandom();
		SlotSymbol a = SlotSymbol.roll(random);
		SlotSymbol b = SlotSymbol.roll(random);
		SlotSymbol c = SlotSymbol.roll(random);
		int payout = SlotSymbol.payout(a, b, c);

		serverLevel.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 0.8F);

		MutableComponent line = Component.empty()
				.append(Component.literal("[ ").withStyle(ChatFormatting.GRAY))
				.append(a.display())
				.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
				.append(b.display())
				.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
				.append(c.display())
				.append(Component.literal(" ]  ").withStyle(ChatFormatting.GRAY));

		Direction facing = state.getValue(FACING);

		if (payout > 1) {
			line.append(Component.translatable("message.villagercasino.win", payout).withStyle(ChatFormatting.GREEN));
			payoutEmeralds(serverLevel, pos, facing, payout);
			serverLevel.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.2F);
			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.4, 0.4, 0.4, 0.0);
		} else if (payout == 1) {
			line.append(Component.translatable("message.villagercasino.refund").withStyle(ChatFormatting.YELLOW));
			payoutEmeralds(serverLevel, pos, facing, 1);
			serverLevel.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
		} else {
			line.append(Component.translatable("message.villagercasino.lose").withStyle(ChatFormatting.RED));
			serverLevel.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 1.0F);
		}

		player.displayClientMessage(line, true);
		return InteractionResult.SUCCESS;
	}

	private static void payoutEmeralds(ServerLevel level, BlockPos pos, Direction facing, int amount) {
		int remaining = amount;
		while (remaining > 0) {
			int stackSize = Math.min(remaining, 64);
			Block.popResourceFromFace(level, pos, facing, new ItemStack(Items.EMERALD, stackSize));
			remaining -= stackSize;
		}
	}
}
